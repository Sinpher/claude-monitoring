/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        "cm-bg": "#0d1117",
        "cm-card": "#161b22",
        "cm-border": "#30363d",
        "cm-text": "#e0e0e0",
        "cm-muted": "#888888",
        "cm-working": "#4CAF50",
        "cm-done": "#2196F3",
        "cm-waiting": "#FF9800",
        "cm-idle": "#9E9E9E",
      },
    },
  },
  plugins: [],
};
