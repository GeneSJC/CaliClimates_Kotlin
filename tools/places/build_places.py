#!/usr/bin/env python3
"""Builds the app's places list (app/src/main/assets/places.csv) from three public US Census files, with no hand-picking.

Inputs, all from https://www2.census.gov and all public domain, put in one folder (the script reads the zip files as they are):
  2023_Gaz_place_national.zip   Gazetteer: every place's id, type and a centre point (the point is not always on the land:
                                San Francisco's is at sea, west of the Farallon Islands)
  ca2020.pl.zip                 2020 Census redistricting file: the population of every place, communities included
  cb_2023_06_place_500k.zip     Cartographic boundaries: used to put each place's point at the middle of its largest piece of
                                land instead

The rule: every California place with at least MIN_POPULATION people, towns and unincorporated communities alike. Run it again
whenever the Census files or the cut-off change; nothing is edited by hand.

Usage:  python3 -I tools/places/build_places.py <folder with the three zip files> <output csv>
Standard library only.
"""
import csv
import io
import struct
import sys
import zipfile
from pathlib import Path

MIN_POPULATION = 1000

GAZETTEER = "2023_Gaz_place_national.zip"
REDISTRICTING = "ca2020.pl.zip"
BOUNDARIES = "cb_2023_06_place_500k.zip"


def read_populations(folder: Path) -> dict:
    """GEOID (7 digits) -> total population, from the redistricting file's geographic header and its first data file."""
    with zipfile.ZipFile(folder / REDISTRICTING) as z:
        logrec = {}
        for line in io.TextIOWrapper(z.open("cageo2020.pl"), encoding="latin-1"):
            f = line.rstrip("\n").split("|")
            if f[2] == "160":  # summary level 160: places
                logrec[f[7]] = f[9]
        pop = {}
        for line in io.TextIOWrapper(z.open("ca000012020.pl"), encoding="latin-1"):
            f = line.split("|")
            if f[4] in logrec:
                pop[logrec[f[4]]] = int(f[5])  # P0010001, total population
    return pop


def read_gazetteer(folder: Path) -> dict:
    """GEOID -> (kind, latitude, longitude) for California places; the point here is the fallback."""
    out = {}
    with zipfile.ZipFile(folder / GAZETTEER) as z:
        name = z.namelist()[0]
        for line in io.TextIOWrapper(z.open(name), encoding="utf-8"):
            f = [x.strip() for x in line.split("\t")]
            if f[0] != "CA":
                continue
            kind = "community" if f[4] == "57" else "city"  # LSAD 57 is a census-designated place
            out[f[1]] = (kind, float(f[10]), float(f[11]))
    return out


def read_dbf(data: bytes):
    """Rows of a dBase III file as dicts of text."""
    n, header_len, record_len = struct.unpack("<xxxxIHH", data[:12])
    fields = []
    pos = 32
    while data[pos] != 0x0D:
        name = data[pos:pos + 11].split(b"\0")[0].decode("ascii")
        length = data[pos + 16]
        fields.append((name, length))
        pos += 32
    rows = []
    for i in range(n):
        rec = data[header_len + i * record_len: header_len + (i + 1) * record_len]
        off = 1
        row = {}
        for name, length in fields:
            row[name] = rec[off:off + length].decode("utf-8", "replace").strip()
            off += length
        rows.append(row)
    return rows


def ring_centroid(points):
    """(signed area, centroid x, centroid y) of one ring."""
    a = cx = cy = 0.0
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        cross = x0 * y1 - x1 * y0
        a += cross
        cx += (x0 + x1) * cross
        cy += (y0 + y1) * cross
    if a == 0:
        return 0.0, points[0][0], points[0][1]
    return a / 2, cx / (6 * (a / 2)), cy / (6 * (a / 2))


def read_shapes(data: bytes):
    """For each polygon record of a shapefile, the centroid (x, y) of its largest outer ring."""
    out = []
    pos = 100
    while pos < len(data):
        _, length = struct.unpack(">ii", data[pos:pos + 8])
        body = data[pos + 8: pos + 8 + length * 2]
        pos += 8 + length * 2
        shape_type = struct.unpack("<i", body[:4])[0]
        if shape_type == 0:
            out.append(None)
            continue
        num_parts, num_points = struct.unpack("<ii", body[36:44])
        parts = struct.unpack("<%di" % num_parts, body[44:44 + 4 * num_parts])
        pts_at = 44 + 4 * num_parts
        pts = [struct.unpack("<dd", body[pts_at + 16 * i: pts_at + 16 * i + 16]) for i in range(num_points)]
        best = None
        for k, start in enumerate(parts):
            end = parts[k + 1] if k + 1 < num_parts else num_points
            area, cx, cy = ring_centroid(pts[start:end])
            if area < 0:  # outer rings run clockwise
                if best is None or abs(area) > abs(best[0]):
                    best = (area, cx, cy)
        out.append((best[1], best[2]) if best else None)
    return out


def read_boundaries(folder: Path) -> dict:
    """GEOID -> (clean name, longitude, latitude of the middle of the largest piece of land)."""
    with zipfile.ZipFile(folder / BOUNDARIES) as z:
        base = [n for n in z.namelist() if n.endswith(".shp")][0][:-4]
        shapes = read_shapes(z.read(base + ".shp"))
        rows = read_dbf(z.read(base + ".dbf"))
    return {r["GEOID"]: (r["NAME"], s[0], s[1]) for r, s in zip(rows, shapes) if s}


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(2)
    folder, out_path = Path(sys.argv[1]), Path(sys.argv[2])
    pop = read_populations(folder)
    gaz = read_gazetteer(folder)
    bounds = read_boundaries(folder)
    rows = []
    for geoid, (kind, lat, lon) in gaz.items():
        people = pop.get(geoid, 0)
        if people < MIN_POPULATION:
            continue
        name, bx, by = bounds.get(geoid, (None, None, None))
        if name is None:
            continue  # no boundary, so no clean name; none of the places above the cut-off lack one
        rows.append((geoid, name, kind, round(by, 5), round(bx, 5), people))
    rows.sort(key=lambda r: -r[5])
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["id", "name", "kind", "latitude", "longitude", "population"])
        w.writerows(rows)
    print("wrote %d places (population %d or more) to %s" % (len(rows), MIN_POPULATION, out_path))


if __name__ == "__main__":
    main()
