package dev.codex.playwright.core;

import java.nio.file.Path;
import java.util.List;

public record ArtifactBundle(Path directory, List<Path> files) {
  public ArtifactBundle {
    files = List.copyOf(files);
  }
}

