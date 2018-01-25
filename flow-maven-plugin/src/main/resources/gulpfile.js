'use strict';

const bundle = {bundle};
const shellFile = "{shell_file}";
const fragmentFiles = [{fragment_files}];
const es6SourceDirectory = "{es6_source_directory}";
const targetDirectory = "{target_directory}";
const es5ConfigurationName = "{es5_configuration_name}";
const es6ConfigurationName = "{es6_configuration_name}";

const fs = require('fs');
const path = require('path');
const del = require('del');
const gulp = require('gulp');
const gulpIf = require('gulp-if');
const gulpRename = require('gulp-rename');
const gulpIgnore = require('gulp-ignore');
const gulpReplace = require('gulp-string-replace');
const polymerBuild = require('polymer-build');
const mergeStream = require('merge-stream');
const Transform = require('stream').Transform;

const File = require('vinyl');
const parse5 = require('parse5');

const cssSlam = require('css-slam');
const htmlMinifier = require('html-minifier');
const babelCore = require('babel-core');
const babelTransform = function (contents, options) {
    return babelCore.transform(contents, options).code;
};

function build(transpileJs, configurationName) {
    const workingDirectory = __dirname;
    const polymerProperties = {
        root: workingDirectory,
        entrypoint: shellFile,
        shell: shellFile,
        fragments: fragmentFiles
    };
    if (fs.existsSync(es6SourceDirectory, 'bower_components', 'webcomponentsjs')) {
        polymerProperties.extraDependencies = [`${es6SourceDirectory}/bower_components/webcomponentsjs/*.js`, `${es6SourceDirectory}/bower_components/webcomponentsjs/*.js.map`];
    }
    const polymerProject = new polymerBuild.PolymerProject(polymerProperties);

    buildConfiguration(polymerProject, path.relative(workingDirectory, es6SourceDirectory), path.join(targetDirectory, configurationName), transpileJs, bundle);
}

function buildConfiguration(polymerProject, redundantPathPrefix, configurationTargetDirectory, transpileJs, bundle) {
    return new Promise((resolve, reject) => {
        console.log(`Deleting ${configurationTargetDirectory} directory...`);
        const buildBundler = new FlowBuildBundler(polymerProject.config, polymerProject.analyzer);
        del([configurationTargetDirectory])
            .then(() => {
                const htmlSplitter = new polymerBuild.HtmlSplitter();

                let initialStream = mergeStream(polymerProject.sources(), polymerProject.dependencies()).pipe(htmlSplitter.split());
                if (transpileJs) {
                    initialStream = initialStream.pipe(gulpIf(/\.js$/, new SafeTransform('babel', babelTransform, {plugins: ['babel-plugin-external-helpers'], presets: ['babel-preset-es2015']})));
                }

                const processedStream = initialStream
                    .pipe(gulpIf(/\.js$/, new SafeTransform('babel', babelTransform, {minified: true, presets: ['babel-preset-minify']})))
                    .pipe(gulpIf(/\.html$/, new SafeTransform('html-minify', htmlMinifier.minify, {collapseWhitespace: true, removeComments: true, minifyCSS: true})))
                    .pipe(gulpIf(/\.css$/, new SafeTransform('css-slam', cssSlam.css)))
                    .pipe(htmlSplitter.rejoin())
                    .pipe(bundle ? buildBundler : gulpIgnore.exclude(file => { return file.path === shellFile } ));

                const nonSourceUserFilesStream = gulp.src([`${es6SourceDirectory}/**/*`, `!${es6SourceDirectory}/**/*.{html,css,js}`]);
                const buildStream = mergeStream(processedStream, nonSourceUserFilesStream)
                    .pipe(gulpRename(path => { path.dirname = path.dirname.replace(redundantPathPrefix, "") }))
                    .pipe(gulpReplace('assetpath="'+redundantPathPrefix+'/', 'assetpath="',{ logs: { enabled: false } }))
                    .pipe(gulp.dest(configurationTargetDirectory));

                return new Promise((resolve, reject) => {
                    buildStream.on('end', resolve);
                    buildStream.on('error', reject);
                });
            })
            .then(() => {
                if (bundle) {
                    writeBundleInformation(buildBundler, redundantPathPrefix, path.join(configurationTargetDirectory, 'vaadin-flow-bundle-manifest.json'));
                }
            })
            .then(() => {
                console.log(`Build for directory ${configurationTargetDirectory} complete!`);
                resolve();
            });
    });
}

function writeBundleInformation(buildBundler, redundantPathPrefix, outputFile) {
    const manifestOutput = fs.openSync(outputFile, 'w');
    fs.writeSync(manifestOutput, bundleMapToJson(buildBundler.manifest.bundles));
    fs.closeSync(manifestOutput);

    function bundleMapToJson(bundleMap) {
        const result = {};
        bundleMap.forEach((bundleObject, bundleUrl) => {
            result[bundleUrl] = [...bundleObject.files, ...bundleObject.inlinedScripts, ...bundleObject.inlinedStyles || []]
                .filter(value => value !== bundleUrl)
                .map(value => value.replace(redundantPathPrefix, ''))
                .map(value => value.startsWith('/') ? value.substring(1) : value);
        });
        return JSON.stringify(result);
    }
}

class SafeTransform extends Transform {
    constructor(optimizerName, optimizer, optimizerOptions) {
        super({objectMode: true});
        this.optimizerName = optimizerName;
        this.optimizer = optimizer;
        this.optimizerOptions = optimizerOptions;
    }

    _transform(file, _encoding, callback) {
        // Do not process webcomponentsjs library
        if (!file.path || file.path.indexOf('webcomponentsjs') >= 0) {
            callback(null, file);
            return;
        }

        if (file.contents) {
            try {
                const contents = this.optimizer(file.contents.toString(), this.optimizerOptions);
                file.contents = new Buffer(contents);
            } catch (error) {
                console.error(`${this.optimizerName}: Unable to optimize ${file.path} , skipping the file.`);
                console.error(`error: ${error.stack}`);
            }
        }
        callback(null, file);
    }
}

function posixPath(filePath) {
    if (path.sep === '\\') {
        filePath = filePath.replace(/\\/g, '/');
    }
    return filePath;
}

function pathFromUrl(root, url) {
    return path.normalize(decodeURI(path.posix.join(posixPath(root), path.posix.join('/', url))));
}

class FlowBuildBundler extends polymerBuild.BuildBundler {

    async _buildBundles() {
        await this._bundler.analyzer.filesChanged(this._getFilesChangedSinceInitialAnalysis());
        const {documents, manifest} = await this._bundler.bundle(await this._generateBundleManifest());
        this.manifest = manifest;
        this._unmapBundledFiles(manifest);
        for (const [filename, document] of documents) {
            this._mapFile(new File({
                path: pathFromUrl(this.config.root, filename),
                contents: new Buffer(parse5.serialize(document.ast)),
            }));
        }
    }
}

gulp.task('build_es6', function () {
    build(false, es6ConfigurationName)
});

gulp.task('build_es5', function () {
    console.log('Starting ES5 transpilation.');
    build(true, es5ConfigurationName)
    console.log('ES5 transpilation completed.');
});

process.on('unhandledRejection', (error, p) => {
    console.error('Failed to process frontend files.');
    console.error(`error: ${error.stack}`);
    process.exit(1)
});
