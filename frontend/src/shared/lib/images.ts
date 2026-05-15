export function referenceImageSrc(value: unknown) {
  const record = value && typeof value === "object" ? value as Record<string, unknown> : {};
  const imageValue = record.referenceImageUrl ?? record.imageUrl ?? record.thumbnailUrl ?? record.photoUrl;
  return typeof imageValue === "string" && imageValue.trim() ? imageValue.trim() : "";
}
