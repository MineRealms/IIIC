#!/usr/bin/env python3
"""
Generate pollution overlay button textures for XaerosWorldMap integration.
Creates two 16x16 PNG files: pollution_button_off.png and pollution_button_on.png
"""

from PIL import Image, ImageDraw

def create_pollution_button(enabled: bool, filename: str):
    """
    Create a 16x16 pollution button icon.

    Args:
        enabled: True for "on" state (green), False for "off" state (gray)
        filename: Output filename
    """
    # Create 16x16 image with transparency
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    if enabled:
        # Green/yellow pollution cloud (enabled state)
        # Outer glow
        draw.ellipse([2, 2, 14, 14], fill=(100, 200, 50, 180))
        # Inner highlight
        draw.ellipse([4, 4, 12, 12], fill=(150, 255, 100, 220))
        # Center dot
        draw.ellipse([6, 6, 10, 10], fill=(200, 255, 150, 255))

        # Add small "pollution particles"
        draw.ellipse([3, 8, 5, 10], fill=(120, 220, 80, 200))
        draw.ellipse([11, 6, 13, 8], fill=(120, 220, 80, 200))
        draw.ellipse([7, 3, 9, 5], fill=(120, 220, 80, 200))
    else:
        # Gray pollution cloud (disabled state)
        # Outer glow
        draw.ellipse([2, 2, 14, 14], fill=(80, 80, 80, 180))
        # Inner highlight
        draw.ellipse([4, 4, 12, 12], fill=(120, 120, 120, 220))
        # Center dot
        draw.ellipse([6, 6, 10, 10], fill=(150, 150, 150, 255))

        # Add small "pollution particles"
        draw.ellipse([3, 8, 5, 10], fill=(100, 100, 100, 200))
        draw.ellipse([11, 6, 13, 8], fill=(100, 100, 100, 200))
        draw.ellipse([7, 3, 9, 5], fill=(100, 100, 100, 200))

    # Save the image
    img.save(filename, 'PNG')
    print(f"Created {filename}")

if __name__ == '__main__':
    import os

    # Output directory
    output_dir = 'src/main/resources/assets/integratedindustrialcraft/textures/gui'
    os.makedirs(output_dir, exist_ok=True)

    # Generate both states
    create_pollution_button(False, os.path.join(output_dir, 'pollution_button_off.png'))
    create_pollution_button(True, os.path.join(output_dir, 'pollution_button_on.png'))

    print("\n✅ Pollution button textures generated successfully!")
    print("   - pollution_button_off.png (gray)")
    print("   - pollution_button_on.png (green)")
