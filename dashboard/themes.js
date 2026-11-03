const light = {
  "SUMMER": {
    "primary": "#012D1D",
    "onPrimary": "#FFFFFF",
    "container": "#D9EBDD",
    "onContainer": "#173B2A",
    "background": "#FFF8F3",
    "surface": "#FFF8F3",
    "variant": "#E8E1DB",
    "ink": "#1E1B17",
    "muted": "#414844",
    "outline": "#717973",
    "tertiary": "#FFDBC8",
  },
  "SPRING": {
    "primary": "#5E7C22",
    "onPrimary": "#FFFFFF",
    "container": "#E6F2C9",
    "onContainer": "#283A08",
    "background": "#FFFAF7",
    "surface": "#FFFAF7",
    "variant": "#F1EAE4",
    "ink": "#252016",
    "muted": "#51493D",
    "outline": "#827A69",
    "tertiary": "#C7EEE5",
  },
  "AUTUMN": {
    "primary": "#365B43",
    "onPrimary": "#FFFFFF",
    "container": "#DFEBD9",
    "onContainer": "#193522",
    "background": "#FFF7F0",
    "surface": "#FFF7F0",
    "variant": "#EFE6DE",
    "ink": "#261811",
    "muted": "#55443B",
    "outline": "#8A7366",
    "tertiary": "#D7CCC8",
  },
  "SNOW": {
    "primary": "#24645D",
    "onPrimary": "#FFFFFF",
    "container": "#D9E9E4",
    "onContainer": "#153A34",
    "background": "#F4F9FC",
    "surface": "#F4F9FC",
    "variant": "#DEE7ED",
    "ink": "#131D24",
    "muted": "#3F4B52",
    "outline": "#6E7A82",
    "tertiary": "#B2EBF2",
  },
};
const dark = {
  "SUMMER": [
    "A5D0B9",
    "10291B",
    "294737",
    "D5EEDC",
    "191C18",
    "F3EEE5",
    "30372E",
    "D4DCCF",
    "A0AA9B",
  ],
  "SPRING": [
    "C2F1AD",
    "173316",
    "275426",
    "D8F8C6",
    "112411",
    "E8F5E1",
    "253B24",
    "C8DCC4",
    "8EAE87",
  ],
  "AUTUMN": [
    "FFB68A",
    "4A1F08",
    "6B3216",
    "FFDCC7",
    "26160E",
    "FFF0E8",
    "4A342A",
    "E7CFC3",
    "B89B8E",
  ],
  "SNOW": [
    "B9E4FF",
    "00344E",
    "0A4968",
    "D4EEFF",
    "081B27",
    "E6F5FF",
    "243B48",
    "C3D8E5",
    "8DA8B9",
  ],
};
export function applyTheme(season, appearance) {
  const d = appearance === "DARK" ||
    appearance === "SYSTEM" &&
      matchMedia("(prefers-color-scheme: dark)").matches;
  let c = light[season] || light.SUMMER;
  if (d) {
    const p = dark[season] || dark.SUMMER;
    c = {
      primary: "#" + p[0],
      onPrimary: "#" + p[1],
      container: "#" + p[2],
      onContainer: "#" + p[3],
      background: "#" + p[4],
      surface: "#" + p[4],
      ink: "#" + p[5],
      variant: "#" + p[6],
      muted: "#" + p[7],
      outline: "#" + p[8],
      tertiary: "#" + p[2],
    };
  }
  for (const [key, v] of Object.entries(c)) {
    document.documentElement.style.setProperty("--" + key, v);
  }
  document.documentElement.style.colorScheme = d ? "dark" : "light";
}
