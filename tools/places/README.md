# Places list

`build_places.py` makes `app/src/main/assets/places.csv`, the list of California places the map names and the place card looks up,
from three public US Census files (public domain, no attribution needed). **There is no hand-picking**: the rule is every place
with at least 1,000 people (`MIN_POPULATION` in the script), towns and unincorporated communities alike, so it works the same way
for every region of the state. A short list of names is left out on purpose (`left-out.txt`, one per line). Today that is 1,067 places.

| Census file (from www2.census.gov) | Used for |
|---|---|
| `geo/docs/maps-data/data/gazetteer/2023_Gazetteer/2023_Gaz_place_national.zip` | every place's id and type (city or community) |
| `programs-surveys/decennial/2020/data/01-Redistricting_File--PL_94-171/California/ca2020.pl.zip` | the 2020 population of every place |
| `geo/tiger/GENZ2023/shp/cb_2023_06_place_500k.zip` | the clean name, and the point at the middle of the place's largest piece of land (the Gazetteer's own point is sometimes at sea: San Francisco's was off the Farallon Islands) |

Put the three zip files in one folder and run, from the repository root:

```bash
python3 -I tools/places/build_places.py <folder with the three zip files> app/src/main/assets/places.csv
```

The file has the columns `id,name,kind,latitude,longitude,population`, largest place first, about 56 KB. Run it again when the
Census files or the cut-off change.

## How far the places are from the stations (last run, with the 344 bundled stations)

| The nearest station is farther than | Places (of 1,067) |
|---|---|
| 3 miles | 868 |
| 5 miles | 665 |
| 10 miles | 300 |
| 20 miles | 71 |
| 30 miles | 34 |

Most places have no station of their own, which is why the app finds the nearest one for every place and says how far away it is.
For example, Sausalito's nearest station is Oakland airport (17.6 miles), Tiburon's is Novato / Gnoss Field (17.6 miles), and
Boulder Creek's is Ben Lomond (2.5 miles).
