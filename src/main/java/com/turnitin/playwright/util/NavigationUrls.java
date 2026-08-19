package com.turnitin.playwright.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/** Validates browser navigation targets without exposing their values in errors. */
public final class NavigationUrls {
  private static final Set<String> WEB_SCHEMES = Set.of("http", "https");

  private NavigationUrls() {
  }

  /** Accepts an HTTP(S) base URL or Playwright's neutral {@code about:blank} default. */
  public static String requireBaseUrl(String value) {
    String url = requireText(value, "baseUrl");
    if (url.equalsIgnoreCase("about:blank")) {
      return url;
    }
    return requireAbsoluteWebUrl(url, "baseUrl");
  }

  /** Accepts only a relative URL that Playwright can resolve against BrowserContext.baseURL. */
  public static String requirePageUrl(String value) {
    String pageUrl = requireText(value, "pageUrl");
    URI uri = parse(pageUrl, "pageUrl");
    if (uri.isAbsolute() || uri.getRawAuthority() != null) {
      throw invalid("pageUrl", "must be relative to baseUrl");
    }
    if (uri.getRawPath() == null || uri.getRawPath().isBlank()) {
      throw invalid("pageUrl", "must include a relative path");
    }
    return pageUrl;
  }

  /** Accepts only a complete HTTP or HTTPS URL. */
  public static String requireAbsoluteUrl(String value) {
    return requireAbsoluteWebUrl(requireText(value, "url"), "url");
  }

  /** Resolves a validated relative page URL for logging without changing navigation input. */
  public static String resolvePageUrl(String baseUrl, String pageUrl) {
    String safeBaseUrl = requireBaseUrl(baseUrl);
    String safePageUrl = requirePageUrl(pageUrl);
    return URI.create(safeBaseUrl).resolve(URI.create(safePageUrl)).toString();
  }

  /** Removes all query and fragment content before a URL is written to logs. */
  public static String safeForLog(String value) {
    String text = redactUserInfo(requireText(value, "url"));
    int query = text.indexOf('?');
    int fragment = text.indexOf('#');
    int sensitiveStart;
    if (query < 0) {
      sensitiveStart = fragment;
    } else if (fragment < 0) {
      sensitiveStart = query;
    } else {
      sensitiveStart = Math.min(query, fragment);
    }
    if (sensitiveStart < 0) {
      return text;
    }
    if (query >= 0 && (fragment < 0 || query < fragment)) {
      return text.substring(0, query)
          + "?<redacted>"
          + (fragment >= 0 ? "#<redacted>" : "");
    }
    return text.substring(0, fragment) + "#<redacted>";
  }

  private static String redactUserInfo(String value) {
    int schemeEnd = value.indexOf("://");
    if (schemeEnd < 0) {
      return value;
    }

    int authorityStart = schemeEnd + 3;
    int authorityEnd = value.length();
    for (char separator : new char[] {'/', '?', '#'}) {
      int index = value.indexOf(separator, authorityStart);
      if (index >= 0) {
        authorityEnd = Math.min(authorityEnd, index);
      }
    }
    int userInfoEnd = value.lastIndexOf('@', authorityEnd - 1);
    if (userInfoEnd < authorityStart) {
      return value;
    }
    return value.substring(0, authorityStart)
        + "<redacted>@"
        + value.substring(userInfoEnd + 1);
  }

  /** Backward-compatible validation for navigation targets that may be relative or absolute. */
  public static String requireNavigationTarget(String value) {
    String target = requireText(value, "url");
    URI uri = parse(target, "url");
    if (uri.isAbsolute() || uri.getRawAuthority() != null) {
      return requireAbsoluteWebUrl(target, "url");
    }
    if (uri.getRawPath() == null || uri.getRawPath().isBlank()) {
      throw invalid("url", "must include a path");
    }
    return target;
  }

  private static String requireAbsoluteWebUrl(String value, String name) {
    URI uri = parse(value, name);
    String scheme = uri.getScheme();
    if (scheme == null || !WEB_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
      throw invalid(name, "must be an absolute HTTP or HTTPS URL");
    }

    try {
      uri = uri.parseServerAuthority();
    } catch (URISyntaxException exception) {
      throw invalid(name, "must contain a valid host and port");
    }
    if (uri.getHost() == null || uri.getHost().isBlank()) {
      throw invalid(name, "must contain a valid host");
    }
    if (uri.getPort() > 65_535) {
      throw invalid(name, "contains a port outside the valid range");
    }
    return value;
  }

  private static URI parse(String value, String name) {
    try {
      return new URI(value);
    } catch (URISyntaxException exception) {
      throw invalid(name, "is malformed");
    }
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw invalid(name, "must not be blank");
    }
    return value.trim();
  }

  private static IllegalArgumentException invalid(String name, String reason) {
    return new IllegalArgumentException(name + " " + reason);
  }
}
