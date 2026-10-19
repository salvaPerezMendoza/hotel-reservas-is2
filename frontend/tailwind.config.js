/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        noche: { DEFAULT: '#16213A', 800: '#22304F', 700: '#33426A', 600: '#4A5884' },
        neblina: { DEFAULT: '#EEF1F5', 200: '#E2E7EE', 300: '#CDD5E0' },
        pizarra: { DEFAULT: '#5B6478', 400: '#8A92A3' },
        alba: { DEFAULT: '#E9A93B', 300: '#F3CB82', 600: '#C98A1E', 50: '#FDF5E6' },
        lavanda: { DEFAULT: '#B9AEDC', 100: '#ECE8F7', 600: '#7466A8' },
        exito: { DEFAULT: '#2F7D5B', 50: '#E6F3EC' },
        alerta: { DEFAULT: '#B4532A', 50: '#FBEDE6' },
      },
      fontFamily: {
        titulo: ['Marcellus', 'Georgia', 'serif'],
        sans: ['"Figtree Variable"', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        flotante: '0 18px 40px -18px rgba(22, 33, 58, 0.35)',
      },
    },
  },
  plugins: [],
}
