export const productName = (value: string) =>
  value
    .replace(/（DEMO）/g, "")
    .replace(/\(DEMO\)/g, "")
    .trim();
