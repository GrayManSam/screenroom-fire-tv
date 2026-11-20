const profiles = [
  ["Movies & series", "MovieBox", "Pick up a series or find a movie for tonight."],
  ["Personal media", "Jellyfin", "Browse movies and shows from your own library."],
  ["Video & creators", "YouTube", "Return to your subscriptions, playlists, and videos."],
];

const dock = Array.from(document.querySelectorAll("[data-app]"));
const sound = document.getElementById("sound");
const settings = document.getElementById("settings");
const utilities = [sound, settings];
const feature = document.querySelector(".feature");
const category = document.getElementById("category");
const title = document.getElementById("title");
const description = document.getElementById("description");
const bridge = window.Screenroom;
const preferences = document.getElementById("preferences");
const settingsSound = document.getElementById("settings-sound");
const contrast = document.getElementById("contrast");
const theme = document.getElementById("theme");
const textSize = document.getElementById("text-size");
const back = document.getElementById("back");
const home = document.querySelectorAll(".masthead, .feature, .dock");
const settingsControls = [back, settingsSound, contrast, theme, textSize, document.getElementById("diagnostics")];
const themes = [
  ["app", "Follow app"],
  ["indigo", "Indigo"],
  ["ocean", "Ocean"],
  ["ember", "Ember"],
];
const textSizes = [
  ["standard", "Standard"], ["large", "Large"], ["extra-large", "Extra large"],
];
const savedTextSize = bridge ? bridge.textSize() : "standard";
let textSizeIndex = Math.max(0, textSizes.findIndex(([id]) => id === savedTextSize));
let selected = 0;
let soundsEnabled = bridge ? bridge.soundsEnabled() : true;
let highContrast = bridge ? bridge.highContrast() : false;
const savedTheme = bridge ? bridge.appearance() : "app";
let themeIndex = Math.max(0, themes.findIndex(([id]) => id === savedTheme));

function show(index) {
  selected = index;
  feature.dataset.active = String(index);
  [category.textContent, title.textContent, description.textContent] = profiles[index];
}

function updateSound() {
  sound.setAttribute("aria-pressed", String(soundsEnabled));
  sound.title = soundsEnabled ? "Mute interface sounds" : "Enable interface sounds";
  settingsSound.setAttribute("aria-pressed", String(soundsEnabled));
  settingsSound.querySelector(".value").textContent = soundsEnabled ? "On" : "Off";
}

function clickSound() {
  if (bridge && soundsEnabled) bridge.playClick();
}

dock.forEach((button, index) => {
  button.addEventListener("focus", () => show(index));
  button.addEventListener("click", () => {
    show(index);
    clickSound();
    if (bridge) bridge.openApp(index);
  });
});

function toggleSound() {
  soundsEnabled = !soundsEnabled;
  updateSound();
  if (bridge) bridge.setSoundsEnabled(soundsEnabled);
  clickSound();
}

sound.addEventListener("click", toggleSound);
settingsSound.addEventListener("click", toggleSound);

function updateContrast() {
  document.documentElement.classList.toggle("high-contrast", highContrast);
  contrast.setAttribute("aria-pressed", String(highContrast));
  contrast.querySelector(".value").textContent = highContrast ? "On" : "Off";
}

contrast.addEventListener("click", () => {
  highContrast = !highContrast;
  updateContrast();
  if (bridge) bridge.setHighContrast(highContrast);
  clickSound();
});

function updateTheme() {
  const [id, label] = themes[themeIndex];
  document.documentElement.dataset.theme = id;
  theme.querySelector(".value").textContent = label;
}

theme.addEventListener("click", () => {
  themeIndex = (themeIndex + 1) % themes.length;
  updateTheme();
  if (bridge) bridge.setAppearance(themes[themeIndex][0]);
  clickSound();
});

function updateTextSize() {
  const [id, label] = textSizes[textSizeIndex];
  document.documentElement.dataset.textSize = id;
  textSize.querySelector(".value").textContent = label;
}

textSize.addEventListener("click", () => {
  textSizeIndex = (textSizeIndex + 1) % textSizes.length;
  updateTextSize();
  if (bridge) bridge.setTextSize(textSizes[textSizeIndex][0]);
  clickSound();
});

settings.addEventListener("click", () => {
  clickSound();
  home.forEach((element) => { element.hidden = true; });
  preferences.hidden = false;
  document.getElementById("display-output").textContent = bridge ? bridge.displayOutput() : "Device only";
  document.getElementById("display-hdr").textContent = bridge ? bridge.displayHdr() : "Device only";
  document.getElementById("audio-outputs").textContent = bridge ? bridge.audioOutputs() : "Device only";
  document.getElementById("media-volume").textContent = bridge ? bridge.mediaVolume() : "Device only";
  settingsSound.focus();
});

window.closeSettings = () => {
  if (preferences.hidden) return;
  preferences.hidden = true;
  home.forEach((element) => { element.hidden = false; });
  settings.focus();
};
back.addEventListener("click", () => {
  clickSound();
  window.closeSettings();
});

window.navigate = (direction) => {
  const active = document.activeElement;
  if (!preferences.hidden) {
    if (direction !== "up" && direction !== "down") return;
    const offset = direction === "up" ? -1 : 1;
    const index = settingsControls.indexOf(active);
    settingsControls[Math.max(0, Math.min(settingsControls.length - 1, index + offset))].focus();
    if (document.activeElement.id === "diagnostics") {
      document.activeElement.scrollIntoView({ block: "end" });
    }
    return;
  }
  const utility = utilities.includes(active);
  const portrait = window.matchMedia("(orientation: portrait), (max-width: 540px)").matches;
  if (utility && direction === "down") {
    dock[selected].focus();
  } else if (!utility && direction === "up" && (!portrait || selected === 0)) {
    sound.focus();
  } else {
    const row = utility ? utilities.filter((button) => !button.disabled) : dock;
    const backwards = direction === "left" || (!utility && portrait && direction === "up");
    const forwards = direction === "right" || (!utility && portrait && direction === "down");
    if (!backwards && !forwards) return;
    const current = Math.max(0, row.indexOf(active));
    row[Math.max(0, Math.min(row.length - 1, current + (backwards ? -1 : 1)))].focus();
  }
};

window.activate = () => {
  const target = document.activeElement;
  if (target instanceof HTMLButtonElement) target.click();
};

document.addEventListener("keydown", (event) => {
  if (!["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(event.key)) return;
  event.preventDefault();
  window.navigate(event.key.slice(5).toLowerCase());
});

updateSound();
updateContrast();
updateTheme();
updateTextSize();
show(0);
if (bridge) dock[0].focus();
