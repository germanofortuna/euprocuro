export type StickerSelection = {
  group: string;
  name: string;
  emblem: string;
};

export const STICKERS_CATEGORY = "FIGURINHAS";

export const STICKER_SELECTIONS: StickerSelection[] = [
  { group: "A", name: "México", emblem: "MX" },
  { group: "A", name: "Africa do Sul", emblem: "ZA" },
  { group: "A", name: "Coréia do Sul", emblem: "KR" },
  { group: "A", name: "Republica Tcheca", emblem: "CZ" },
  { group: "B", name: "Canada", emblem: "CA" },
  { group: "B", name: "Bosnia e Herzegovina", emblem: "BA" },
  { group: "B", name: "Catar", emblem: "QA" },
  { group: "B", name: "Suíça", emblem: "CH" },
  { group: "C", name: "Brasil", emblem: "BR" },
  { group: "C", name: "Marrocos", emblem: "MA" },
  { group: "C", name: "Haiti", emblem: "HT" },
  { group: "C", name: "Escócia", emblem: "SCO" },
  { group: "D", name: "Estados Unidos", emblem: "US" },
  { group: "D", name: "Paraguai", emblem: "PY" },
  { group: "D", name: "Australia", emblem: "AU" },
  { group: "D", name: "Turquia", emblem: "TR" },
  { group: "E", name: "Alemanha", emblem: "DE" },
  { group: "E", name: "Curaçao", emblem: "CW" },
  { group: "E", name: "Costa do Marfim", emblem: "CI" },
  { group: "E", name: "Equador", emblem: "EC" },
  { group: "F", name: "Holanda", emblem: "NL" },
  { group: "F", name: "Japão", emblem: "JP" },
  { group: "F", name: "Suécia", emblem: "SE" },
  { group: "F", name: "Tunísia", emblem: "TN" },
  { group: "G", name: "Bélgica", emblem: "BE" },
  { group: "G", name: "Egito", emblem: "EG" },
  { group: "G", name: "Ira", emblem: "IR" },
  { group: "G", name: "Nova Zelândia", emblem: "NZ" },
  { group: "H", name: "Espanha", emblem: "ES" },
  { group: "H", name: "Cabo Verde", emblem: "CV" },
  { group: "H", name: "Arábia Saudita", emblem: "SA" },
  { group: "H", name: "Uruguai", emblem: "UY" },
  { group: "I", name: "França", emblem: "FR" },
  { group: "I", name: "Senegal", emblem: "SN" },
  { group: "I", name: "Iraque", emblem: "IQ" },
  { group: "I", name: "Noruega", emblem: "NO" },
  { group: "J", name: "Argentina", emblem: "AR" },
  { group: "J", name: "Argélia", emblem: "DZ" },
  { group: "J", name: "Áustria", emblem: "AT" },
  { group: "J", name: "Jordânia", emblem: "JO" },
  { group: "K", name: "Portugal", emblem: "PT" },
  { group: "K", name: "RD Congo", emblem: "CD" },
  { group: "K", name: "Uzbequistão", emblem: "UZ" },
  { group: "K", name: "Colômbia", emblem: "CO" },
  { group: "L", name: "Inglaterra", emblem: "ENG" },
  { group: "L", name: "Croácia", emblem: "HR" },
  { group: "L", name: "Gana", emblem: "GH" },
  { group: "L", name: "Panamá", emblem: "PA" }
];

export const SPECIAL_STICKER_SELECTIONS = [
  "Coca-Cola",
  "McDonald's Exclusive",
  "Extra Stickers",
  "Historia da Copa"
];

export function stickerGroups() {
  return STICKER_SELECTIONS.reduce<Record<string, StickerSelection[]>>((groups, selection) => {
    groups[selection.group] = [...(groups[selection.group] ?? []), selection];
    return groups;
  }, {});
}

export function stickerGroupForSelection(value: string) {
  const selection = STICKER_SELECTIONS.find((item) => item.name === value);
  if (selection) {
    return selection.group;
  }
  return SPECIAL_STICKER_SELECTIONS.includes(value) ? "SPECIAL" : "";
}

export function normalizeStickerNumbers(value: string) {
  return value
    .split(/[\s,;]+/)
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean);
}
