package ca.vanzyl.provisio.tools.tool;

import static kr.motd.maven.os.Detector.OS;

import ca.vanzyl.provisio.tools.model.ToolDescriptor;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ToolUrlBuilder {

  public static String interpolateToolPath(String toolPath, ToolDescriptor toolDescriptor, String version, String arch) {
    return interpolateToolPath(toolPath, toolDescriptor, version, OS, arch);
  }

  public static String interpolateToolPath(String toolPath, ToolDescriptor toolDescriptor, String version, String os, String arch) {
    String toolVersion = version != null ? version : toolDescriptor.defaultVersion();
    String mappedOs = mapOs(os, toolDescriptor);
    String mappedArch = mapArch(arch, toolDescriptor);

    return toolPath
        .replaceAll("\\{version\\}", toolVersion)
        .replaceAll("\\{os\\}", mappedOs)
        .replaceAll("\\{arch\\}", mappedArch);
  }

  public static String mapOs(String os, ToolDescriptor toolDescriptor) {
    if (toolDescriptor.osMappings() != null) {
      if (toolDescriptor.osMappings().get(os) != null) {
        os = toolDescriptor.osMappings().get(os);
      }
    }
    return os;
  }

  public static String mapArch(String arch, ToolDescriptor toolDescriptor) {
    if (toolDescriptor.archMappings() != null) {
      if (toolDescriptor.archMappings().get(arch) != null) {
        arch = toolDescriptor.archMappings().get(arch);
      }
    }
    return arch;
  }

  public static String toolDownloadUrlFor(ToolDescriptor toolDescriptor, String version, String arch) {
    return interpolateToolPath(toolDescriptor.downloadUrlTemplate(), toolDescriptor, version, arch);
  }

  public static String toolDownloadUrlFor(ToolDescriptor toolDescriptor, String version, String os, String arch) {
    return interpolateToolPath(toolDescriptor.downloadUrlTemplate(), toolDescriptor, version, os, arch);
  }

  public static Path cachePathFor(Path cacheDirectory, ToolDescriptor tool, String version, String arch) {
    String url = toolDownloadUrlFor(tool, version, arch);
    String file = url.substring(url.lastIndexOf('/') + 1);
    return cachePathFor(cacheDirectory, tool, version, arch, file);
  }

  public static Path cachePathFor(Path cacheDirectory, ToolDescriptor tool, String version, String arch, String fileName) {
    return cacheDirectory.resolve(Paths.get(tool.id(), version)).resolve(fileName);
  }
}
