export const environment = {
  production: true,
  /**
   * Paywall VAT wording next to the price, e.g. 'incl. VAT'. Empty = no VAT line (VAT is not decided yet).
   * The shown price must be the final total.
   */
  vatLabel: '',
  /** Seller name for the paywall small print ("Sold by …"). Empty = the line is left out until it is decided. */
  sellerName: '',
  apiBaseUrl: ''
};
