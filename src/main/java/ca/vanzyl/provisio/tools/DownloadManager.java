package ca.vanzyl.provisio.tools;

import static ca.vanzyl.provisio.tools.ToolUrlBuilder.cachePathFor;
import static ca.vanzyl.provisio.tools.ToolUrlBuilder.toolDownloadUrlFor;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;

import ca.vanzyl.provisio.tools.model.ToolDescriptor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.List;

public class DownloadManager {

  private final Path cacheDirectory;

  public DownloadManager(Path cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  public Path resolve(ToolDescriptor tool, String version) throws Exception {
    // This is constructed from the url template
    String url = toolDownloadUrlFor(tool, version);
    Path target;
    if(tool.fileNameFromContentDisposition()) {
      String fileName = fileNameFromContentDisposition(url);
      target = cachePathFor(cacheDirectory, tool, version, fileName);
    } else {
      target = cachePathFor(cacheDirectory, tool, version);
    }
    if (exists(target)) {
      return target;
    }
    createDirectories(target.getParent());
    // https://www.baeldung.com/java-9-http-client
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(url))
        // client will fallback to http/1.1 if http/2 is not supported
        .version(HttpClient.Version.HTTP_2)
        .GET()
        .build();
    HttpClient client = HttpClient.newBuilder()
        .followRedirects(Redirect.ALWAYS)
        .build();
    HttpResponse<Path> response = client.send(request, BodyHandlers.ofFile(target));
    //HttpResponse<Path> response = client.send(request, BodyHandlers.ofFileDownload(target));
    if (response.statusCode() == 404) {
      throw new RuntimeException(String.format("The URL %s doesn't exist.", url));
    }
    return target;
  }

  private String fileNameFromContentDisposition(String url) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(url))
        // client will fallback to http/1.1 if http/2 is not supported
        .version(HttpClient.Version.HTTP_2)
        .method("HEAD", HttpRequest.BodyPublishers.noBody())
        .build();
    HttpClient client = HttpClient.newBuilder()
        .followRedirects(Redirect.ALWAYS)
        .build();
    HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
    if (response.statusCode() == 404) {
      throw new RuntimeException(String.format("The URL %s doesn't exist.", url));
    }

    // Truly horrible, use streams
    List<String> headers = response.headers().allValues("Content-Disposition");
    for(String header : headers) {
      if(header.contains(";")) {
        String[] parts = header.split(";");
        for(String part : parts) {
          if(part.contains("filename=")) {
            return part.replace("filename=", "").trim();
          }
        }
      }
    }

    return null;
  }
}
