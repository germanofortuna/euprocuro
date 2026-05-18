const MAX_IMAGE_SIDE = 1280;
const IMAGE_QUALITY = 0.82;
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

export type ImageReadOptions = {
  maxBytes?: number;
  maxSide?: number;
  quality?: number;
  outputType?: "image/jpeg" | "image/png" | "image/webp";
  maxOutputLength?: number;
};

export function readImageFile(file: File, options: ImageReadOptions = {}): Promise<string> {
  const maxBytes = options.maxBytes ?? MAX_IMAGE_BYTES;
  const maxSide = options.maxSide ?? MAX_IMAGE_SIDE;
  const quality = options.quality ?? IMAGE_QUALITY;
  const outputType = options.outputType ?? "image/jpeg";

  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    return Promise.reject(new Error("Selecione uma imagem JPG, PNG ou WebP."));
  }
  if (file.size > maxBytes) {
    return Promise.reject(new Error(`A imagem deve ter no maximo ${Math.round(maxBytes / 1024 / 1024)} MB.`));
  }
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error("Nao foi possivel ler a imagem."));
    reader.onload = () => {
      const image = new Image();
      image.onerror = () => reject(new Error("Nao foi possivel processar a imagem."));
      image.onload = () => {
        const scale = Math.min(1, maxSide / Math.max(image.width, image.height));
        const canvas = document.createElement("canvas");
        canvas.width = Math.max(1, Math.round(image.width * scale));
        canvas.height = Math.max(1, Math.round(image.height * scale));
        canvas.getContext("2d")?.drawImage(image, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL(outputType, quality);
        if (options.maxOutputLength && dataUrl.length > options.maxOutputLength) {
          reject(new Error("A imagem ficou grande demais. Tente uma foto menor."));
          return;
        }
        resolve(dataUrl);
      };
      image.src = String(reader.result ?? "");
    };
    reader.readAsDataURL(file);
  });
}
