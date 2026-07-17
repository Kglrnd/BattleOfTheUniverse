import { GameAssetPipe } from './game-asset.pipe';

describe('GameAssetPipe', () => {
  const pipe = new GameAssetPipe();

  it('resolves a numeric planet variant to the planets category path', () => {
    expect(pipe.transform(3, 'planets')).toBe('/images/planets/3.webp');
  });

  it('resolves a string catalog key to the buildings category path', () => {
    expect(pipe.transform('metal_mine', 'buildings')).toBe('/images/buildings/metal_mine.webp');
  });

  it('resolves a string catalog key to the ships category path', () => {
    expect(pipe.transform('light_fighter', 'ships')).toBe('/images/ships/light_fighter.webp');
  });
});
