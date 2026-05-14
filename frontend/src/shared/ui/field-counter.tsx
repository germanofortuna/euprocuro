export function FieldCounter({ value, max }: { value: string; max: number }) {
  return <span className="field-counter">{value.length}/{max}</span>;
}
