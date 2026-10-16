import { resetSaleConfigReportForTests, saleConfig } from './legal-config';
import { FILLED_SALE_CONFIG } from './legal-test-config';

describe('saleConfig (server billingEnabled is the single source of truth)', () => {
  beforeEach(() => resetSaleConfigReportForTests());

  it('server off: hidden, even with a complete seller/VAT config, and nothing is logged', () => {
    const err = spyOn(console, 'error');
    expect(saleConfig(false, FILLED_SALE_CONFIG)).toBeNull();
    expect(err).not.toHaveBeenCalled();
  });

  it('server on + config complete: ready', () => {
    expect(saleConfig(true, FILLED_SALE_CONFIG)!.sellerName).toBe('Test Seller OÜ');
  });

  it('server on + config empty: hidden and console.error once', () => {
    const err = spyOn(console, 'error');
    expect(saleConfig(true, {})).toBeNull();
    expect(saleConfig(true, {})).toBeNull();
    expect(err).toHaveBeenCalledTimes(1);
  });

  it('an environment billingEnabled is not a gate any more', () => {
    expect(saleConfig(false, { ...FILLED_SALE_CONFIG, billingEnabled: true })).toBeNull();
  });
});
