package net.vulkanshaders.model;

import java.util.ArrayList;
import java.util.List;

public class PipelineConfig {

    public String vertex;
    public String fragment;
    public String geometry; // Declared for Future Use

    public List<String> includes = new ArrayList<>();

    public String blend = "opaque"; // opaque, cutout, translucent
    public boolean depthTest = true;
    public boolean depthWrite = true;
    public String cullFace = "back"; // none, front, back

    public String stage = "main"; // main, post_process

}
