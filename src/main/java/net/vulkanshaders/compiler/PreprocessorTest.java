package net.vulkanshaders.compiler;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for GLSL preprocessor (can be removed in production)
 */
public class PreprocessorTest {
    public static void main(String[] args) {
        // Create some test include files
        Map<String, String> includes = new HashMap<>();

        includes.put("shaders/include/common.glsl",
                "#define PI 3.14159\n" +
                        "#define TWO_PI 6.28318\n"
        );

        includes.put("shaders/include/lighting.glsl",
                "#include \"shaders/include/common.glsl\"\n" +
                        "vec3 calculateLighting(vec3 normal, vec3 lightDir) {\n" +
                        "    return max(dot(normal, lightDir), 0.0) * vec3(1.0);\n" +
                        "}\n"
        );

        // Test shader source
        String shaderSource =
                "#version 450\n" +
                        "#include \"shaders/include/lighting.glsl\"\n" +
                        "\n" +
                        "layout(location = 0) in vec3 position;\n" +
                        "layout(location = 1) in vec3 normal;\n" +
                        "\n" +
                        "void main() {\n" +
                        "    vec3 color = calculateLighting(normal, vec3(0, 1, 0));\n" +
                        "    gl_Position = vec4(position, 1.0);\n" +
                        "}\n";

        // Preprocess
        GLSLPreprocessor preprocessor = new GLSLPreprocessor(includes);
        String processed = preprocessor.preprocess(shaderSource, "test.vsh");

        System.out.println("=== PREPROCESSED OUTPUT ===");
        System.out.println(processed);
    }
}
