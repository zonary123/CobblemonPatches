package org.kingpixel.cobblemonpatches.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing color codes (& and §), hex colors (&#RRGGBB),
 * and formatting into Minecraft {@link Text} components.
 */
public final class TextUtils {

  private static final Pattern FORMAT_PATTERN = Pattern.compile("(?i)[&§]#([0-9a-f]{6})|[&§]([0-9a-fk-or])");

  private TextUtils() {
  }

  /**
   * Converts a formatted string with color codes into a Minecraft {@link Text} component.
   *
   * @param input the string containing legacy or hex color codes
   * @return formatted {@link MutableText}
   */
  public static MutableText parse(String input) {
    if (input == null || input.isEmpty()) {
      return Text.empty();
    }

    MutableText root = Text.empty();
    Matcher matcher = FORMAT_PATTERN.matcher(input);
    int lastEnd = 0;
    Style currentStyle = Style.EMPTY;

    while (matcher.find()) {
      if (matcher.start() > lastEnd) {
        String textPart = input.substring(lastEnd, matcher.start());
        root.append(Text.literal(textPart).setStyle(currentStyle));
      }

      String hex = matcher.group(1);
      String legacy = matcher.group(2);

      if (hex != null) {
        int rgb = Integer.parseInt(hex, 16);
        currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
      } else if (legacy != null) {
        currentStyle = applyLegacyCode(currentStyle, legacy.charAt(0));
      }

      lastEnd = matcher.end();
    }

    if (lastEnd < input.length()) {
      String remaining = input.substring(lastEnd);
      root.append(Text.literal(remaining).setStyle(currentStyle));
    }

    return root;
  }

  private static Style applyLegacyCode(Style style, char code) {
    Formatting formatting = Formatting.byCode(Character.toLowerCase(code));
    if (formatting == null || formatting == Formatting.RESET) {
      return Style.EMPTY;
    }
    if (formatting.isColor()) {
      return Style.EMPTY.withFormatting(formatting);
    }
    return style.withFormatting(formatting);
  }
}
