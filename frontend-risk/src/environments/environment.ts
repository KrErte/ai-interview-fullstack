export const environment = {
  production: false,
  /**
   * Paywall VAT wording next to the price, e.g. 'incl. VAT'. Empty = no VAT line (VAT is not decided yet).
   * The shown price must be the final total.
   */
  vatLabel: '',
  /** Seller name for the paywall small print ("Sold by …"). Empty = the line is left out until it is decided. */
  sellerName: '',
  /**
   * Client-side billing flag for the legal pages: the /terms sale section and the sale-specific /refund
   * content render only when this is true AND the seller fields below are filled in. Default false.
   */
  billingEnabled: false,
  /** Seller details for /terms §1, /refund and the withdrawal form; empty = not decided (sale content hidden). */
  sellerAddress: '',
  sellerEmail: '',
  /** Optional: registry code (company) and VAT number (VAT-registered seller); rows are left out while empty. */
  sellerRegCode: '',
  sellerVatNumber: '',
  /** "Last updated" date of the terms of sale and refund page, e.g. '16 October 2026'; empty hides the sale content. */
  legalLastUpdated: '',
  apiBaseUrl: 'http://localhost:8080'
};
