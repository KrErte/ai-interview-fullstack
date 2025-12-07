/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts,scss}"
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['system-ui', 'ui-sans-serif', 'SF Pro Text', 'Inter', 'sans-serif'],
      },
      colors: {
        // valikuline, sul juba kasutusel slate/emerald jne, siia saad oma customid panna
      },
    },
  },
  plugins: [],
};
