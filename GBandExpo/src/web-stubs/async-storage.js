// Web stub for AsyncStorage — uses localStorage
const AsyncStorage = {
  getItem: (key) => Promise.resolve(localStorage.getItem(key)),
  setItem: (key, value) => { localStorage.setItem(key, value); return Promise.resolve(); },
  removeItem: (key) => { localStorage.removeItem(key); return Promise.resolve(); },
  clear: () => { localStorage.clear(); return Promise.resolve(); },
  getAllKeys: () => Promise.resolve(Object.keys(localStorage)),
  multiGet: (keys) => Promise.resolve(keys.map(k => [k, localStorage.getItem(k)])),
  multiSet: (pairs) => { pairs.forEach(([k, v]) => localStorage.setItem(k, v)); return Promise.resolve(); },
};

export default AsyncStorage;
