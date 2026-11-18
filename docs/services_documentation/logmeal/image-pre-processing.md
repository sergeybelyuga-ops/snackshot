> Source: https://docs.logmeal.com/docs/guides-essential-concepts-image-pre-processing

# Image Pre-processing

> This section helps ensure that your input images are optimized for LogMeal’s recognition models, improving both accuracy and reliability while staying within API limits.

Before sending food images to the **LogMeal API**, follow these best practices to improve recognition accuracy. Proper pre-processing reduces errors, speeds up processing, and improves the quality of the results.

## Why Pre-processing Matters

The API relies on computer vision models trained on millions of images. Clear, well-structured input images lead to:

- More accurate dish recognition
- Better nutritional information extraction
- Faster response times

## General Guidelines

- **Lighting:** Ensure good, natural lighting. Avoid shadows covering the food.
- **Focus:** Images should be sharp and not blurry.
- **Framing:** The food should take up most of the image area.
- **Multiple dishes:** If multiple dishes are present, ensure each one is clearly visible.
- **Convert to JPG:** Always convert images to `.jpg`. JPG is the standard input format for the LogMeal API and provides broad compatibility, consistent server-side decoding, and optimized compression for photographic content.
- **Reduce file size:** Images must be smaller than 1 MB to ensure faster uploads, reduce timeouts and bandwidth, and stay within upload limits.
- **Limit image dimensions:** The longest side must be no more than 1000 pixels. Resize larger originals before applying the file-size reduction loop below.
- **Avoid clutter:** Background objects such as cutlery and menus may confuse the model.

## Depth & Sequence Capture

Some advanced use cases, such as Food Quantity Detection or Food Waste Detection, benefit from extra capture modes:

- **Depth images:** If the device supports depth sensors, provide paired RGB and depth images for better quantity estimation.
- **Sequence capture:** For leftovers or waste detection, send before-and-after meal images.

See [Food Quantity Detection](./food-quantity-detection.md) for the supported capture method.

## Sample Images

### Good capture

- Bright, centered dish on a plate
- Minimal background noise

Recommended sample images:

- [Pudding](https://api.logmeal.com/assets/sample_images/pudding.jpg)
- [Lentils](https://api.logmeal.com/assets/sample_images/lentils.jpeg)
- [Flan](https://api.logmeal.com/assets/sample_images/flan.jpeg)

### Poor capture

- Dark image with shadows
- Dish partly cropped
- Multiple overlapping plates
- Blurry image
- Occlusions
- Top-down or otherwise unsuitable framing

Sample images:

- [Blurry image](https://api.logmeal.com/assets/sample_images/blurry.jpeg)
- [Occlusions](https://api.logmeal.com/assets/sample_images/occlusions.jpeg)
- [Cropped egg](https://api.logmeal.com/assets/sample_images/egg.jpeg)
- [Top-view coffee](https://api.logmeal.com/assets/sample_images/coffee_top.jpeg)

## Code Examples

The following snippets preserve EXIF orientation and standardize uploads to no more than 1000 pixels per side and less than 1 MB.

### Convert to JPG while preserving EXIF orientation (Python)

```python
import os
from PIL import Image
import piexif

path_original_image = '<path-of-the-original-image>'
global_path = '<output-folder-path-ending-with-/> '

im = Image.open(path_original_image)
exif_dict = piexif.load(im.info.get('exif', b''))
exif_bytes = piexif.dump(exif_dict)
rgb_im = im.convert('RGB')

image_name = os.path.basename(path_original_image)
image_name_noext = os.path.splitext(image_name)[0]
path_jpg = os.path.join(global_path, image_name_noext + '.jpg')

rgb_im.save(path_jpg, 'JPEG', exif=exif_bytes, quality=90)
print('Saved:', path_jpg)
```

### Resize to <= 1000 px per side (Python)

```python
import os
from PIL import Image
import piexif

MAX_SIDE = 1000

path_in = "<path-to-image>"
path_out = "<path-to-output-jpg>"

im = Image.open(path_in)
exif_dict = piexif.load(im.info.get("exif", b""))
exif_bytes = piexif.dump(exif_dict)

rgb_im = im.convert("RGB")
w, h = rgb_im.size
scale = min(1.0, MAX_SIDE / float(max(w, h)))
if scale < 1.0:
    new_w, new_h = int(w * scale), int(h * scale)
    rgb_im = rgb_im.resize((new_w, new_h), Image.LANCZOS)

rgb_im.save(path_out, "JPEG", exif=exif_bytes, quality=90, optimize=True)
print("Saved:", path_out, "| size:", Image.open(path_out).size)
```

### Reduce image file size to < 1MB (Python)

```python
import os
from PIL import Image
import piexif

path = '<path-to-jpg>'
im = Image.open(path)
exif_dict = piexif.load(im.info.get('exif', b''))
exif_bytes = piexif.dump(exif_dict)
rgb_im = im.convert('RGB')

size_mb = lambda p: os.path.getsize(p) >> 20
width, height = rgb_im.size

while size_mb(path) >= 1:
    width, height = int(width * 0.75), int(height * 0.75)
    rez_image = rgb_im.resize((width, height), Image.ANTIALIAS)
    rez_image.save(path, 'JPEG', exif=exif_bytes, optimize=True, quality=85)

print('Final size (MB):', size_mb(path))
```

### Quick ways to shrink images

#### Node.js (Sharp)

```js
import sharp from "sharp";

const MAX_SIDE = 1000;

await sharp("input.jpg")
  .rotate()
  .resize({ width: MAX_SIDE, height: MAX_SIDE, fit: "inside", withoutEnlargement: true })
  .jpeg({ quality: 85, mozjpeg: true })
  .toFile("output.jpg");
```

#### Browser (Canvas)

```js
async function resizeImageFile(file, maxSide = 1000, quality = 0.85) {
  const img = document.createElement("img");
  img.src = URL.createObjectURL(file);
  await new Promise((r) => (img.onload = r));

  const scale = Math.min(1, maxSide / Math.max(img.width, img.height));
  const w = Math.round(img.width * scale);
  const h = Math.round(img.height * scale);

  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  canvas.getContext("2d").drawImage(img, 0, 0);

  const blob = await new Promise((resolve) =>
    canvas.toBlob(resolve, "image/jpeg", quality)
  );

  return new File([blob], file.name.replace(/\.[^.]+$/, ".jpg"), { type: "image/jpeg" });
}
```

#### Command line (ImageMagick)

```bash
magick input.jpg -auto-orient -resize 1000x1000\> -quality 85 -strip output.jpg
```

### Recommended preprocessing pipeline

1. Convert to JPG while keeping EXIF orientation.
2. Resize so the longest side is no more than 1000 px.
3. If needed, iteratively downscale or compress until the file is less than 1 MB.

This approach preserves orientation, uses a progressive quality-preserving downscale loop, avoids `413 Request Entity Too Large`, and keeps inference latency low.

## Next Steps

- [User Types & Access Tokens](./users-and-access-tokens.md)
- [Plans & Limits](../logmeal-plans-limits.md)
- [Food Recognition Capabilities](./recognition-capabilities.md)
