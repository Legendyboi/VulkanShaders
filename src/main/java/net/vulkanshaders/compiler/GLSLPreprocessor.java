package net.vulkanshaders.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Preprocesses GLSL shader source code, handling #include directives
 */
public class GLSLPreprocessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("VulkanShaders/Preprocessor");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("^\\s*#\\s*include\\s+[\"<]([^\"'>]+)[\">]");

    private final Map<String, String> availableIncludes;
    private final Set<String> processedIncludes; // Track to prevent circular includes

    public GLSLPreprocessor(Map<String, String> availableIncludes) {
        this.availableIncludes = availableIncludes;
        this.processedIncludes = new HashSet<>();
    }

    /**
     * Preprocess shader source, resolving all #include directives
     *
     * @param source The shader source code
     * @param shaderPath The path of the shader being processed (for error messages)
     * @return Preprocessed source with includes expanded
     */
    public String preprocess(String source, String shaderPath) {
        processedIncludes.clear();
        return preprocessRecursive(source, shaderPath, 0);
    }

    /**
     * Recursively preprocess shader source
     */
    private String preprocessRecursive(String source, String currentPath, int depth) {
        if (depth > 32) {
            throw new RuntimeException("Include depth exceeded 32 levels - possible circular include at: " + currentPath);
        }

        StringBuilder result = new StringBuilder();
        String[] lines = source.split("\n");
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            Matcher matcher = INCLUDE_PATTERN.matcher(line);

            if (matcher.find()) {
                String includePath = matcher.group(1);

                // Check for circular includes
                if (processedIncludes.contains(includePath)) {
                    LOGGER.warn("Skipping already included file: {} (referenced in {})",
                            includePath, currentPath);
                    result.append("// ").append(line).append(" [already included]\n");
                    continue;
                }

                // Get include content
                String includeContent = availableIncludes.get(includePath);

                if (includeContent == null) {
                    throw new RuntimeException(String.format(
                            "Include not found: '%s' (referenced in %s at line %d)",
                            includePath, currentPath, lineNumber
                    ));
                }

                // Mark as processed
                processedIncludes.add(includePath);

                // Add line marker for debugging
                result.append(String.format("// BEGIN INCLUDE: %s\n", includePath));

                // Recursively preprocess the included file
                String processedInclude = preprocessRecursive(includeContent, includePath, depth + 1);
                result.append(processedInclude);

                // Ensure newline at end
                if (!processedInclude.endsWith("\n")) {
                    result.append("\n");
                }

                result.append(String.format("// END INCLUDE: %s\n", includePath));

                LOGGER.debug("Included: {} -> {}", currentPath, includePath);
            } else {
                // Regular line, keep as-is
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Extract all #include directives from source (for dependency analysis)
     */
    public static List<String> extractIncludes(String source) {
        List<String> includes = new ArrayList<>();
        String[] lines = source.split("\n");

        for (String line : lines) {
            Matcher matcher = INCLUDE_PATTERN.matcher(line);
            if (matcher.find()) {
                includes.add(matcher.group(1));
            }
        }

        return includes;
    }
}
