#!/bin/bash
# Generates App Deck icon (PNG + ICNS)
# Requires: Java 21+, sips, iconutil (macOS)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Write temp Java program to generate the icon
TMP_DIR=$(mktemp -d)
cat > "$TMP_DIR/GenIcon.java" << 'EOF'
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenIcon {
    static BufferedImage create(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = size / 32;
        g.setColor(new Color(22, 22, 34));
        g.fillRoundRect(pad, pad, size - pad * 2, size - pad * 2, size / 6, size / 6);

        int cols = 5, rows = 3, gap = size / 21;
        int cw = (size - pad * 2 - gap * (cols + 1)) / cols;
        int ch = (size - pad * 2 - gap * (rows + 1)) / rows;
        int ox = pad + gap, oy = pad + gap;

        int[][] colors = {
            {0x4A90D9, 0x50C878, 0xE8A040, 0xD95959, 0x9B59B6},
            {0x1ABC9C, 0xE67E22, 0x3498DB, 0xE74C3C, 0x2ECC71},
            {0xF39C12, 0x2980B9, 0x8E44AD, 0x27AE60, 0xD35400}
        };

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = ox + c * (cw + gap);
                int y = oy + r * (ch + gap);
                Color base = new Color(colors[r][c]);
                GradientPaint gp = new GradientPaint(x, y, base.brighter(), x, y + ch, base.darker());
                g.setPaint(gp);
                g.fillRoundRect(x, y, cw, ch, size / 32, size / 32);
            }
        }

        g.dispose();
        return img;
    }

    public static void main(String[] args) throws Exception {
        ImageIO.write(create(1024), "png", new File(args[0]));
    }
}
EOF

javac "$TMP_DIR/GenIcon.java" -d "$TMP_DIR"

OUT="$SCRIPT_DIR/app-icon.png"
java -cp "$TMP_DIR" GenIcon "$OUT"
rm -rf "$TMP_DIR"

echo "PNG generated: $OUT"

# Create .iconset folder for iconutil
ICONSET="$SCRIPT_DIR/App Deck.iconset"
mkdir -p "$ICONSET"

for size in 16 32 64 128 256 512; do
    sips -z $size $size "$OUT" --out "$ICONSET/icon_${size}x${size}.png" > /dev/null
    if [ "$size" -le 256 ]; then
        sips -z $((size * 2)) $((size * 2)) "$OUT" --out "$ICONSET/icon_${size}x${size}@2x.png" > /dev/null
    fi
done

# Add 1024x1024 as @2x of 512
cp "$OUT" "$ICONSET/icon_512x512@2x.png"

iconutil -c icns "$ICONSET" -o "$SCRIPT_DIR/app-icon.icns"
rm -rf "$ICONSET"

echo "ICNS generated: $SCRIPT_DIR/app-icon.icns"
echo "Done!"
