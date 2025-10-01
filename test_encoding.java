import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.Json;

public class TestEncoding {
    public static void main(String[] args) {
        // Test array encoding
        var array = JsonUtils.createArray(Json.create("string"), Json.create(true));
        var encoded = JsonCodec.encodeWithTypeInfo(array);
        System.out.println("Array: " + encoded.toJson());
    }
}
EOF < /dev/null