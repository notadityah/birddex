/**
 * Client-side image resizer for bird photos before upload/detection.
 * - maxDim=1280: large enough for good detection accuracy, small enough for fast upload.
 * - quality=0.85: JPEG compression — good visual quality with ~60-70% size reduction.
 * - JPEG format: universal browser support, good compression for photos.
 * Returns { base64, blob, previewUrl } — base64 for detect API, blob for S3 upload.
 */
export function useImageResize() {
  async function resizeImage(file, maxDim = 1280, quality = 0.85) {
    const url = URL.createObjectURL(file)
    try {
      const img = await loadImage(url)
      const { width, height } = scaleDimensions(img.width, img.height, maxDim)

      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, width, height)

      const blob = await new Promise((resolve, reject) => {
        canvas.toBlob(
          (b) => (b ? resolve(b) : reject(new Error('Canvas export failed'))),
          'image/jpeg',
          quality,
        )
      })

      const base64 = await blobToBase64(blob)
      const previewUrl = URL.createObjectURL(blob)

      return { base64, blob, previewUrl }
    } finally {
      URL.revokeObjectURL(url)
    }
  }

  function loadImage(src) {
    return new Promise((resolve, reject) => {
      const img = new Image()
      img.onload = () => resolve(img)
      img.onerror = () => reject(new Error('Failed to load image'))
      img.src = src
    })
  }

  function scaleDimensions(w, h, max) {
    if (w <= max && h <= max) return { width: w, height: h }
    const ratio = Math.min(max / w, max / h)
    return { width: Math.round(w * ratio), height: Math.round(h * ratio) }
  }

  function blobToBase64(blob) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => {
        // Strip data URL prefix to get raw base64
        const result = reader.result
        resolve(result.split(',')[1])
      }
      reader.onerror = () => reject(new Error('Failed to read blob'))
      reader.readAsDataURL(blob)
    })
  }

  return { resizeImage }
}
