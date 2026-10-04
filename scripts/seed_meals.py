import re, json, sys
p = "shared/seed_meals.json"
d = json.load(open(p))
assert d["version"] == 1
for m in d["meals"]:
    assert m["slug"] and m["name"] and m["diet"] in ("vegetarian", "non_veg", "eggetarian")
    assert m["ingredients"] and m["steps"]
    for i in m["ingredients"]:
        assert i["qtyPerServing"] > 0 and i["unit"] in ("g", "ml", "pc", "tsp", "tbsp", "cup")
print(f"OK: {len(d['meals'])} meals valid (sample seed; expand to ~100 before closed-track).")
