package com.vaadin.flow.spring.springnative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.PWA;

public class VaadinBeanFactoryInitializationAotProcessor
        implements BeanFactoryInitializationAotProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static class Marker {

    }

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            ConfigurableListableBeanFactory beanFactory) {
        // Find and register @Route classes so they can be created as beans at
        // runtime
        if (beanFactory instanceof BeanDefinitionRegistry) {
            findAndRegisterRoutes(
                    (BeanDefinitionRegistry & BeanFactory) beanFactory);
        } else {
            logger.error(
                    "Unable to register @Route classes as beans because the used bean factory is of type {} which does not implement {}",
                    beanFactory.getClass().getName(),
                    BeanDefinitionRegistry.class.getName());
        }

        return (generationContext, beanFactoryInitializationCode) -> {
            var hints = generationContext.getRuntimeHints();
            for (var pkg : getPackages(beanFactory)) {
                var reflections = new Reflections(pkg);

                /*
                 * This aims to register most types in the project that are
                 * needed for Flow to function properly. Examples are @Route
                 * annotated classes, Component and event classes which are
                 * instantiated through reflection etc
                 */

                for (var c : getRouteTypesFor(reflections, pkg)) {
                    registerType(hints, c);
                    registerResources(hints, c);
                }
                boolean hasPWA = false;
                for (var c : reflections
                        .getSubTypesOf(AppShellConfigurator.class)) {
                    registerType(hints, c);
                    registerResources(hints, c);
                    hasPWA = hasPWA || c.getAnnotation(PWA.class) != null;
                }
                if (hasPWA) {
                    hints.jni().registerType(
                            TypeReference.of("java.lang.System"),
                            MemberCategory.INVOKE_PUBLIC_METHODS);
                    for (String cls : getJNIClassesForPWA()) {
                        hints.jni().registerType(TypeReference.of(cls),
                                MemberCategory.values());
                    }
                }

                registerSubTypes(hints, reflections, Component.class);
                registerSubTypes(hints, reflections, RouterLayout.class);
                registerSubTypes(hints, reflections, HasErrorParameter.class);
                registerSubTypes(hints, reflections, ComponentEvent.class);
                registerSubTypes(hints, reflections, HasUrlParameter.class);
                registerSubTypes(hints, reflections,
                        "com.vaadin.flow.data.converter.Converter");
            }
        };
    }

    private void registerSubTypes(RuntimeHints hints, Reflections reflections,
            Class<?> cls) {
        for (var c : reflections.getSubTypesOf(cls)) {
            registerType(hints, c);
        }
    }

    private void registerSubTypes(RuntimeHints hints, Reflections reflections,
            String className) {
        try {
            Class<?> cls = Class.forName(className);
            for (var c : reflections.getSubTypesOf(cls)) {
                registerType(hints, c);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Ignore. this happens for e.g. Converter in a Hilla project where
            // you do not
            // have flow-data
        }
    }

    private static List<String> getPackagesWithRoutes(BeanFactory beanFactory) {
        List<String> packages = new ArrayList<String>();
        packages.add("com.vaadin");
        packages.addAll(AutoConfigurationPackages.get(beanFactory));
        return packages;
    }

    private <T extends BeanFactory & BeanDefinitionRegistry> void findAndRegisterRoutes(
            T beanFactory) {
        String markerBeanName = Marker.class.getName();
        logger.debug("Finding and registering routes");

        if (beanFactory.containsBeanDefinition(markerBeanName)) {
            logger.debug("Routes already registered");
            return;
        }

        Set<String> registeredClasses = new HashSet<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            // Routes can be manually registered using @Component.
            // We should not register those again
            BeanDefinition def = beanFactory.getBeanDefinition(beanName);
            if (def.getBeanClassName() != null) {
                registeredClasses.add(def.getBeanClassName());
            }
        }

        for (String pkg : getPackagesWithRoutes(beanFactory)) {
            logger.debug("Scanning for @{} or @{} annotated beans in {}",
                    Route.class.getSimpleName(),
                    RouteAlias.class.getSimpleName(), pkg);
            var reflections = new Reflections(pkg);
            for (var c : getRouteTypesFor(reflections, pkg)) {
                if (registeredClasses.contains(c.getName())) {
                    logger.debug(
                            "Skipping route class {} as it has already been registered as a bean",
                            c.getName());
                    continue;
                }
                registeredClasses.add(c.getName());
                logger.debug("Registering a bean for route class {}",
                        c.getName());
                AbstractBeanDefinition beanDefinition = createPrototypeBeanDefinition(
                        c);
                beanFactory.registerBeanDefinition(c.getName(), beanDefinition);

                // Layouts classes are instantiated programmatically, and they
                // might need to be
                // managed by Spring (e.g. because of @PostConstruct annotated
                // methods)
                Set<Class<? extends RouterLayout>> definedLayouts = new HashSet<>();
                if (c.isAnnotationPresent(Route.class)) {
                    definedLayouts.add(c.getAnnotation(Route.class).layout());
                } else if (c.isAnnotationPresent(RouteAlias.class)) {
                    definedLayouts
                            .add(c.getAnnotation(RouteAlias.class).layout());
                } else if (c.isAnnotationPresent(RouteAlias.Container.class)) {
                    for (RouteAlias alias : c
                            .getAnnotation(RouteAlias.Container.class)
                            .value()) {
                        definedLayouts.add(alias.layout());
                    }
                }
                definedLayouts.removeIf(
                        layout -> registeredClasses.contains(layout.getName())
                                || layout == RouterLayout.class
                                || UI.class.isAssignableFrom(layout));
                for (Class<? extends RouterLayout> layout : definedLayouts) {
                    beanFactory.registerBeanDefinition(layout.getName(),
                            createPrototypeBeanDefinition(layout));
                    registeredClasses.add(layout.getName());
                }

            }
        }

        beanFactory.registerBeanDefinition(markerBeanName, BeanDefinitionBuilder
                .rootBeanDefinition(Marker.class).getBeanDefinition());

    }

    private static AbstractBeanDefinition createPrototypeBeanDefinition(
            Class<?> c) {
        return BeanDefinitionBuilder.rootBeanDefinition(c).setScope("prototype")
                .getBeanDefinition();
    }

    private static Collection<Class<?>> getRouteTypesFor(
            Reflections reflections, String packageName) {
        var routeTypes = new HashSet<Class<?>>();
        routeTypes.addAll(reflections.getTypesAnnotatedWith(Route.class));
        routeTypes.addAll(reflections.getTypesAnnotatedWith(RouteAlias.class));
        routeTypes.addAll(reflections.getTypesAnnotatedWith(Layout.class));
        return routeTypes;
    }

    private void registerResources(RuntimeHints hints, Class<?> c) {
        if (c.getCanonicalName() == null) {
            // See
            // https://github.com/spring-projects/spring-framework/issues/29774
            return;
        }
        hints.resources().registerType(c);
    }

    private void registerType(RuntimeHints hints, Class<?> c) {
        if (c.getCanonicalName() == null) {
            // See
            // https://github.com/spring-projects/spring-framework/issues/29774
            return;
        }
        MemberCategory[] memberCategories = MemberCategory.values();
        hints.reflection().registerType(c, memberCategories);
    }

    private static List<String> getPackages(BeanFactory beanFactory) {
        var listOf = new ArrayList<String>();
        listOf.add("com.vaadin");
        listOf.addAll(AutoConfigurationPackages.get(beanFactory));
        return listOf;
    }

    // List taken from AwtProcessor in Quarkus AWT extension
    private static String[] getJNIClassesForPWA() {
        return new String[] { "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "com.sun.imageio.plugins.jpeg.JPEGImageWriter",
                "java.awt.GraphicsEnvironment", "java.awt.AlphaComposite",
                "java.awt.Color", "java.awt.color.CMMException",
                "java.awt.color.ColorSpace", "java.awt.color.ICC_ColorSpace",
                "java.awt.color.ICC_Profile", "java.awt.color.ICC_ProfileGray",
                "java.awt.color.ICC_ProfileRGB", "java.awt.Composite",
                "java.awt.geom.AffineTransform", "java.awt.geom.GeneralPath",
                "java.awt.geom.Path2D", "java.awt.geom.Path2D$Float",
                "java.awt.geom.Point2D$Float",
                "java.awt.geom.Rectangle2D$Float",
                "java.awt.image.AffineTransformOp",
                "java.awt.image.BandedSampleModel",
                "java.awt.image.BufferedImage", "java.awt.image.ColorModel",
                "java.awt.image.ComponentColorModel",
                "java.awt.image.ComponentSampleModel",
                "java.awt.image.ConvolveOp", "java.awt.image.DirectColorModel",
                "java.awt.image.IndexColorModel", "java.awt.image.Kernel",
                "java.awt.image.MultiPixelPackedSampleModel",
                "java.awt.image.PackedColorModel",
                "java.awt.image.PixelInterleavedSampleModel",
                "java.awt.image.Raster", "java.awt.image.SampleModel",
                "java.awt.image.SinglePixelPackedSampleModel",
                "java.awt.Insets", "java.awt.Rectangle",
                "java.awt.Transparency", "java.awt.Toolkit",
                "javax.imageio.IIOException",
                "javax.imageio.plugins.jpeg.JPEGHuffmanTable",
                "javax.imageio.plugins.jpeg.JPEGQTable",
                "sun.awt.image.BufImgSurfaceData",
                "sun.awt.image.BufImgSurfaceData$ICMColorData",
                "sun.awt.image.ByteBandedRaster",
                "sun.awt.image.ByteComponentRaster",
                "sun.awt.image.ByteInterleavedRaster",
                "sun.awt.image.BytePackedRaster",
                "sun.awt.image.DataBufferNative",
                "sun.awt.image.GifImageDecoder",
                "sun.awt.image.ImageRepresentation", "sun.awt.image.ImagingLib",
                "sun.awt.image.IntegerComponentRaster",
                "sun.awt.image.IntegerInterleavedRaster",
                "sun.awt.image.ShortBandedRaster",
                "sun.awt.image.ShortComponentRaster",
                "sun.awt.image.ShortInterleavedRaster",
                "sun.awt.image.SunWritableRaster",
                "sun.awt.image.WritableRasterNative", "sun.awt.SunHints",
                "sun.font.CharToGlyphMapper", "sun.font.Font2D",
                "sun.font.FontConfigManager",
                "sun.font.FontConfigManager$FcCompFont",
                "sun.font.FontConfigManager$FontConfigFont",
                "sun.font.FontConfigManager$FontConfigInfo",
                "sun.font.FontManagerNativeLibrary", "sun.font.FontStrike",
                "sun.font.FreetypeFontScaler", "sun.font.GlyphLayout",
                "sun.font.GlyphLayout$EngineRecord",
                "sun.font.GlyphLayout$GVData",
                "sun.font.GlyphLayout$LayoutEngine",
                "sun.font.GlyphLayout$LayoutEngineFactory",
                "sun.font.GlyphLayout$LayoutEngineKey",
                "sun.font.GlyphLayout$SDCache",
                "sun.font.GlyphLayout$SDCache$SDKey", "sun.font.GlyphList",
                "sun.font.PhysicalStrike", "sun.font.StrikeMetrics",
                "sun.font.TrueTypeFont", "sun.font.Type1Font",
                "sun.java2d.cmm.lcms.LCMS",
                "sun.java2d.cmm.lcms.LCMSImageLayout",
                "sun.java2d.cmm.lcms.LCMSProfile",
                "sun.java2d.cmm.lcms.LCMSTransform",
                "sun.java2d.DefaultDisposerRecord", "sun.java2d.Disposer",
                "sun.java2d.InvalidPipeException", "sun.java2d.NullSurfaceData",
                "sun.java2d.SurfaceData", "sun.java2d.loops.Blit",
                "sun.java2d.loops.BlitBg", "sun.java2d.loops.CompositeType",
                "sun.java2d.loops.DrawGlyphList",
                "sun.java2d.loops.DrawGlyphListAA",
                "sun.java2d.loops.DrawGlyphListLCD",
                "sun.java2d.loops.DrawLine",
                "sun.java2d.loops.DrawParallelogram",
                "sun.java2d.loops.DrawPath", "sun.java2d.loops.DrawPolygons",
                "sun.java2d.loops.DrawRect",
                "sun.java2d.loops.FillParallelogram",
                "sun.java2d.loops.FillPath", "sun.java2d.loops.FillRect",
                "sun.java2d.loops.FillSpans",
                "sun.java2d.loops.GraphicsPrimitive",
                "sun.java2d.loops.GraphicsPrimitiveMgr",
                "sun.java2d.loops.MaskBlit", "sun.java2d.loops.MaskFill",
                "sun.java2d.loops.ScaledBlit", "sun.java2d.loops.SurfaceType",
                "sun.java2d.loops.TransformHelper",
                "sun.java2d.loops.XORComposite",
                "sun.java2d.pipe.BufferedMaskBlit",
                "sun.java2d.pipe.GlyphListPipe", "sun.java2d.pipe.Region",
                "sun.java2d.pipe.RegionIterator",
                "sun.java2d.pipe.ShapeSpanIterator",
                "sun.java2d.pipe.SpanClipRenderer",
                "sun.java2d.pipe.SpanIterator", "sun.java2d.pipe.ValidatePipe",
                "sun.java2d.SunGraphics2D" };
    }

}
