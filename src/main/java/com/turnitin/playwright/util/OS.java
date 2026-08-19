package com.turnitin.playwright.util;

public final class OS {

  private static final Platform PLATFORM = detect();

  private OS() {}

  private static Platform detect() {

    String os = System.getProperty("os.name").toLowerCase();

    if (os.contains("win"))
      return Platform.WINDOWS;

    if (os.contains("nux") || os.contains("nix") || os.contains("aix"))
      return Platform.LINUX;

    if (os.contains("mac"))
      return Platform.MAC;

    return Platform.UNKNOWN;
  }

  public static Platform platform() {
    return PLATFORM;
  }

  public static boolean isWindows() {
    return PLATFORM == Platform.WINDOWS;
  }

  public static boolean isLinux() {
    return PLATFORM == Platform.LINUX;
  }

  public static boolean isMac() {
    return PLATFORM == Platform.MAC;
  }
}
