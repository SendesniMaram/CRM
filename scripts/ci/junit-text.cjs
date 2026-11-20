// XML 1.0 forbids these characters, although security tests may include them
// intentionally in a test title. Keep a visible escape in the report.
function xmlSafeText(value) {
  return String(value).replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\ufffe\uffff]/g,
    char => `\\u${char.charCodeAt(0).toString(16).padStart(4, '0')}`);
}

module.exports = { xmlSafeText };
