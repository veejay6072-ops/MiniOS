package opersys.multiprogramming;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/** The Multiprogramming app icon (blue tile with three bars), embedded so no image file is needed. */
public final class MultiprogrammingIcon {
    private static final String DATA =
            "iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAeIklEQVR42u2da4xd13Xff+vcM8PhDF9DUiIpSiIlUpYqW5YpSrYiR25Mp46SKj" +
            "FQxYmrOkidGH0GSIs2X5r2Q9GiQFD0g9uiLYpaaGDZcBOrgdzESQxHUWRLlmxRiqwHJUs0KT5EUiJnhuQM53Xv+ffDOefefZ733Dv33DFpXmDA" +
            "Ga5z99prr3323mut/1rbJNHtEz9hwOwSfPu4ePJtOPiOODsPEmAYih41ANYhPgTcj7EfcQuwHWMSGCGPraUYFtEs9Uy/NJz/swK+pGh57avH/h" +
            "fTljGmEaeBN4GDGN8G/hox6/AyMxQIrh2HfduNT9xkfPQGWD+aFLfbx6/wDAYEgpfOiD96Qzz1tjg9ZzRMNDxMQmAy5AEbJJsEXW/wAYldYFsN" +
            "TYSKNxMKZYl6Gf9ujoRS+LtZ9Fw0wwAUlNDi78XtuwMht/2It0raKGpfFXlbF95k2jAFNgKaMGNrNHYzFn75hGTToAsGQRCAmdmZOekbb4nvnz" +
            "Lu2wmffr+xb5tVUn7IsGQFiGfR1AI8/oZ47JB4+7xA0PDMhOJ3fgxjG7AXsQ/jLmAPYjMwgTEOrEH4WDTpVPAWtmnRf5bR+lkBeuFdW/ulvJug" +
            "JrAIXMKYA6YQhzFeQLwIvAWcARaiiWTLgQnErVuNB/cav/g+45rx7itB+QQQvH4OvvxywJNvB0zNGyONkBB9aw1wA3A32AFgP2g72CQw1pn2iV" +
            "Gy7MbS7whW0UK/tH73nxXzTnXAIoZaAJsGToMOAk+APQ8cBy1aNBNagZgYgZ/Z7fGb+zzet7l8EmQmQPxwAHz3uPjC98Rr7wlDeNZe7DxgB3Av" +
            "8EvAR8B2AOuiFqJmZI6wFi6P7TYyA5GlDXYClPGuewL0wdvdwKKNqv3yzAKnwJ4Dvg561rBTgsAQrXCR5sPXGf/k7gb3XFc8CRITIP6tFcATRw" +
            "P+8/cC3jwnxvxwwwvC3Wot8AHQpwUPGHaT0IR13nQjPA1afC5M70hqD0h8eiTnuQ41PjO4A1VO67RJmwOZPtB3+y6tuM/5nMmRuxJvRU9Hy6rM" +
            "YA7sCPBnwB8KXjGYj7kHMm7dAr/1YY+/ucvDLDsJMitAIPjWkYD/+r2Aw1PC98K5F2l3K3AA+Gz09m9OTeeqZ4+rn5V9lPp3CngWeDTcGjhrdC" +
            "yyGzca/+xej4/v8vC8pJJ8t0UJnjke8IVnA47OiIaFEwLwgBuBhyLl3waMOEq37v21PmgrGR8bUl+GySt9jmr/xyTws6Drwa4HHhMcAwIT/GhK" +
            "/LfvBWwcNe7ZaQnOvruTHTorvvBsi7fOiTE/VH608dws+PsGnwF2x0pX54SSEUWZ3Vu5tJC3KrbRC02Z3Ta7AeS1oT54l8tW7FfRimSzzssnYF" +
            "Rwh6F/rHBC/G+DwwL5Bq+/J/77Qdgy3uDmSSMQeAZe3JnpefjyD1q8+m7AmkZb+Z7BHuA3DD6rHOXn+UaU2hOUGgRLLB3KVZJSNPVJs4Lht0wf" +
            "s8rvVTYVyJZ3pByEbClD1SJ/wW6Dzxr8BqHuPAG+B8+fbPE/D7Z4dy5UrOIVoBWIPzoU8MSRIHJOKN7zbxT8OvAwcF2keI+C87IKfidnEhR/k5" +
            "x3saj9arR++PUrmyryYkCypScBKIh09TDQAr4IvB2plT99s8XeSfj1fT4jXjgR+MG74muvNpmeD/f96LM12vM/Y7ATaBA9X+Yx7EazLjQraXMQ" +
            "vOtsn1WWLfp4ka52Rlv2QxbqEgOWmvC1V8OVXoB/fkH839davH1ejHjtQ9+Yc9rfHb31Xn/vVf6xtVda3bwH0T4/PrLFutpNuHWfAB4HFnyDt2" +
            "fgD15psXuT4T99LODJIy0CKXaNG/BB4Nei035b+Vc/l9Un1tltkS6PAM9jKAA9e7zFsyc8Gjse/F1eeCdgNArqmLHD4HOIXwQmzPAsOnu0HQk5" +
            "U9KlpZ/rl+Y6QleDd1H76f4Nk3cf7XtmbDFYRLyKuOh52KVlWFgWjdGP/yvmlhQf6UeBTwCfB24k8gAXnYpEdkBybRlVoBW0P3DeCcdHf+3LnQ" +
            "gaLu/S0HWWd3w0GAM2AYcRh4FWS7DYBP/sXEDDorc/PPV/CrjZOuZee4YlwqvRfhEzdWltO9elqQItp32V0OLGCnk74ZXCsH2ebI7zNU+2DE09" +
            "yE01uSvJVk3u+L9utlC3L0m85YHNLJr8oGNXjAnuBu4B1mZC6UodPApOMJn/rplW3VQroankwKWSA9flIFtnfqxVqNu7CQ+FCwvLAV682hhsAw" +
            "5YaEPKmZClpgw5tDKTZ5C0It6DoPXy3GUgWzwJrousu21xmNZHilfZPcB+wbr0KtirqdKvqdcrbQBvx0+KbPF8WBdiNtgTxQrkR9HAjcBdYNsd" +
            "/BL9AfYuR9pPhGzxKrA91DUvgM140SObLUTzTFpK++YEPJJLVTIglVxyLBFcKaepgNb5bhGNHFraB28l/vl+ZWPFsnWXu5ps1qtsitBa+w02g/" +
            "CRxhW6eveEBwUFicNfqT9fJf7wqrQiP3f3v/J9+PkIJ0sFgrLvSJomh9ugZesud1XZymmJPsbDsBa0J9L5aR+xH7hDMGlJS6IL2EkDpVlOmHYl" +
            "NKzjCgtcsKdrJyXsaUvaVw56RhqM3IOSrRottx8xqG8yDB2DL3Q/2C5gQkiWgia5rCzVnBKMLCdoqa607u1bz7yJQl/zLfA9Y3wkjmKpBNivTG" +
            "B4dgmCQIw2XIhZR5VVZUurYpXHVYIJsDuFNvlI+wldhWsVzwApiamzKETsviQW48TCAcnQoqUkS4v2JSW7qhjlVkhzkHF5vB28fVMBE77Hx3b7" +
            "/NSNDTaPe0kMDdVwN1NzAc8ca/HMsSatVjQmkXeqityZI9gKZEvS8r4HCb05Y5KiCVhrxq2StvrALYgJhTBur+2Bcvcr0eMeaG0IY4amkhi6nD" +
            "lsKXdwm1zAW+E70ArEulHjM3eO8mv7Rrluw8riWJ98X8CXXljiqy8tstBU+4g8ULkryJa/8Du0dPvK5W3RzBqTuAHY7AM7hHzAt848yyBejEQ+" +
            "QDSTKaCpmJbZt5S/p8nF+GZ5J4YkolkEh7h7Z4PP7V/DteuMlvpHHArYucHjc3ev4eh0k2+91WTEC9HRleSmotwVZIvbL6RloGzK70e4BPigzQ" +
            "YbPGCTifUWooPM9RzEHbIiD1NFmtSdRhUaBTR10tfGR4z7bxrhmolwSDzr/6dh0BJsW2fcv3uEtX7aqqgmm61Etgpy59HSunF0aiZ8g/XAJl+S" +
            "n+ccFzY4lITb3kpoJX2MJ4A/Ajs3RMofQFA9PhZOrjXWrYGzcyFqSsOSjR500xutAeC3N/ieIiT9TYKB0Mr62Db1rO9lv9Afb51lt7QfdchGD7" +
            "rpkeb1ErbWAGgU0LRC3omjlajnIxX2v4rcqzGu3Xj7aaGGFWzRAHBzeRNc6ibNynJxlDNedQSZhhXguor16ych6wr6xNFArEtlkKIiGL3S8rNn" +
            "slkwvbbfde0bgPaF4pyJwjzfMrlXY1y78far4tFtQLSiHIGVtm9DeFuMZC7WIHD+dY9rN95+lRpBl80KrU4Jl8G3rc7PFbQb+Ffexqaa276yxs" +
            "tTEfxayTBo+rn2iVs5NAfgmNe+Ctonr/2KvOXmuNc8t4pk6yb3aowrXcbVj59WiS2R67uwEho90JQyA9PtqxpvnGpftb370RaQ8alUkG01x5WS" +
            "cfWumms9NqsrzQxMQ6HiQ5TlmB1ya+L1QAsMs2SY0tKJGS4UKV2PLxWgyqNlXgmr7wyQ8J47smXHxCmikJNXUVW2KjRkyKUV6VRdzUAnbFSAm+" +
            "uZZsUFIvJ4tcObPdBsiN6ahBloZXKXzEMrMuvU15jEhS2LaEU6XYEZaDUPdtXarcNzBVc1A6V2hZW+6gfGwSfP6hjnZHt+bvJjpRVU1FtGSb3T" +
            "6rbSctpPl4VpCbau89i3c5Tt692qXJaLCrZMkRpjaq7FK6eWOT4Tpu0PtiSV8vwAlgNDXGkxRRfW2S+tuFCkdeVd5wywTP9DOBrccd0I/+C+dd" +
            "x9/Sjr1lhhSZWinkow34TDZ5s88tws33xjAc8pxVWmm+q0zrjmbAGit7iTDShe1U8bqbdKSV/94PcAUltAZzI2Ja5d5/MvP76ej+0ZWxGniTWw" +
            "dWKUTWs3MDMf8MzRRUYbhjToGKHiLaCuzLghZ97VvgUoNzU6AEYa8Mnb1vDhG9e0H+trLYrmdCC49Vqfn79tjOeOLoaYQg2qcE2H7nP1syLfQg" +
            "xFG/WM27eNMDZiK7dCHQvhpi0+16zzODcX1HLsvooHGMShOvpnzLeBG6IjDWO0YXHxrvrwAL0aX90uzqhKG1T7QdsMrA0PFp7Ic8zARDyipihk" +
            "Ht9BjKufcM257s6cg3VuNYsqtF7a7+RX9NS+WU60xeqBhRQfQ+q2QQc/rl6hn0Illp+6IBe7tVGVRkXeNiQnoFQNeVnnGWTA4xpuAd0U3SvToE" +
            "uHVJiTWa70LrxFfVuAEkty8oV3Q7jUtQ1EVoDU47h2sbz9ZDD5MoYFq+ZluH2+UEE/anZAFeH9VwgL9q5Ic01XGixY9aOCa7eWzFZ29KrQTamz" +
            "XNZ3DCiwAuri2z7TKon0GRwmkKGYge0M3j7atz7ubqzrRVyNO3GsD31UNwOlgThtiwwBDJqt8O+G13v7EjQD4XkWfl/F1VnTufl1vIq5K2atWE" +
            "R14ss2+AykWl3BQXQo27ahwU1bRtk80Si/cCBn5jYDceZiix+9t8z0fAu/WwNXYPZOnWfMWs8ADc+47+a1/MOf3sRdN4wy5vd35pxdEt86NMf/" +
            "euY8h04vFRoLdXsC3cyg7BkgqCcfId4+g7AkzODPANLAixSG4VG4Z9cYv/vAFm7fProiz9ymtcYv37WeDWsb/LtvnOVHZ5v4jaSyraiI76Cti7" +
            "zxahfisTozXmopgukN9l6MCBUDTIwYn7xtPFT+CpewuPX7947x0T1rUdcE6LoMsUqOiJqBcb3qqPz57BZgJQeaRG3zVApWXIkqOvStX9fg1u0j" +
            "Azmdx2zXjnjsuWaEiVFjfinA8+IXw6IDWrQqqL6zWFwtLZkXoAixS635CLLUFpeuc9+H3rzsXbKlN00nEappxKrTa89o19cb5GcktgYSt+sqY3" +
            "nUdQ4w8i8Wr2sFUFHZVrK1lYr1VmxSemk3owjcPCuHlixwpoAkLUgNRN1mUbsiUsjbXIdRrdlhqSKsNTNMlqVUQjcKggp6izFLdL7j6M3L9t1K" +
            "tjhVS5kZepRMQ4oG9mB81+YFLpLVymkFx4PoDNAJIUWFQp3Z1kkmS4CXpUQhwk4LNe/FjkMm5tMBhKh9FqgNC9COBjpyt0Eb9YWh2+ansmNeqr" +
            "cUzZSscOZDcaXJLC1bKCmfVm/CiHKKNplU82upjJydSUGttamKVrrcMc/VW7p6aOd7Xp2+69VwyqluQAirVPmCywQTmE6jrssp0tkCkpjAepEZ" +
            "ZLB5lvBAqi6WuR7IQQSDPCupH1NG66c2Tp2rjQ2Ztw0zCllwKZcGoLfECjAocE98GKsTItUubKGcN1F1ZIkJKcjUCTQXLSRqTElXaVHNfvXmXa" +
            "0J8ZMtnY9UMWm5Gs01W2pZF1NngMq+igEGg3JZSvXCAaUV6yaP5idMBEtZEO7fPdBU8/vYPg4FHd4mtzZAvSFhF5Nvw8gJiFHBbn2fAn1kaF30" +
            "5uVCs60wGzpLo+RS3rri4yVlMev0AqikerNqnwRdxpyCfIkuOvXjpdpyThjtWe7g3s1SpXgsSYvvp1HNWbqJal0JZ2a92TlyZJOGVJlKndpEco" +
            "N7qRpECVqZTp3n2qlhKk+LLy5/n2rc6sZmOzZ3bom0YSwBXe4yqmXjkfLzPFS8QqqCb8m7/Gp1rh42fzV4183Ty3WsqB4Y80CdP1WXrMs6EaU6" +
            "/Nf6pBXDwjUIR01N8Cw3RUs5lbxq34/zYIGqFZDa7Qof9UnzJQZYtT68mKhOdK7r8WtXB09kD1mtiCBFF+u5ZWmlAAKrd5GJLn1SZX1Ue85Lv0" +
            "JWsvYU+5WVuS28zsEo5k2tLmiiyyndV8pWLRpYro+qz2VuDBFZDGxehTuRl+oVuWiCepdDF4CRWMEcgEhtzhg5DhkXt1/bFtDZTuVcpJl3G3mS" +
            "VqTTpN78LIpIJbW4utOM+s2xhPLT9i+1gYKTPOWaaFb76Te+GrfaRfPVdepRCHRTyRG3W0L/MI/GGiYipFhmqVMYo3aLJ2+M1RfNj5fMZC3K5H" +
            "ITAy6MJPYPZ8lJtFGnBRDf0SsllnylizjUdQB0glBtueMAkdUV+5IThOpcYW/tO8bj28YDh1as09j+D+Jq4crcbY+jXHOQeOaAENOHjPi6Vg2l" +
            "hLSl/vbafakXom3O5czmcLQhOBwMI4hUbYmKyXLhn3TTqTuN/ASkULnXV2f3ktxqVMoppqn66uU4xRqc+OBQCoXGyOfQ4lSUva1E4kxd5me2Op" +
            "gSelNab7k67ejGQyVXjim5D1n6+8q/xdo0RE/gauD0U1uqDaF4ftERqKve8midnBrnziDLv4OmTSuIQ1uKZuYEa2q9Hi5wSoN1+libB1JuJDL/" +
            "sqbaIeE5lcASeiujFdwr5Gfx5L35ovMal+KqmvVOfxcnZ7WBUZxK+0oBQhJbQ1B7NFDk1qruq2zIZXxpVJc3vL7k4NIRre/8ofoxgXWs0VZzxy" +
            "UVmLsa1kkw6SKWuByTQ7y64vm1rv6iVnOvF6HqTkijh7zbfmiFNYK04oIGbqSuhkRJ1ymTdprUYXm4dwfn5CPUmZGUvgllkJPAK8o6KctIqZq5" +
            "MqxSLatRuy8j95CKR1fVTTd9WF5ewABKzybTpWvzASQvDU74ompzQSvDN8lSteZCpi9+XKkxIGcCNCMrqjGIhAO5nTbViM0MEnV5ak/QaJsYQX" +
            "RLp3ujp5C82hzAsRkYW5q96KPkuZaBPGBGcNGg6TqLcy9iM2c5sRLaEOB5wjoeTstDzwz0sjDHAMjz/SdDZAOFgyp7CDTnRtgifaR1Q+c5CZoG" +
            "F4EZX9IpYEIw1q4cqpIlJM8L5tDieNTiUoupi8sDc5HGAs3MLTO/1AqDTkpafiZYbga8e2HZiYmt7Iyg6KB0/lKL2YVmqP4glaYtaLXEuzPLnQ" +
            "octvKV3zO4uLDM+blmBwzW5YbwMr2pEw9qCqaAOQ94EzgutAAEcXQhVRLKaaQLTcL3jOlLTb7zwwvML4e3XbWC0DvYz08zEJ7Byeklnjs8y/xS" +
            "gFnYWbcfnsGlxYAnD80wfam5Yr6tIKx2euFSi++8cZ6LCy08S77roTtcLDRbPPn6ec5cWA77ppXwFQ3PuLQY8NTrF5i6tJyo7pYef+VVA8lUDW" +
            "mHsAJgQeg48GZjbP9v7kU0MW4G1lviiuLkWmIyZzmxRAKoS/PMWA4Czlxosm6Nx/t3jjPie5hZXz+eGedmmzzyV2f4+gvnWG4FqRJpHd6B4Mz5" +
            "ZcZGPG7fOc74aGMFfOH8fJNHn36PLz/zLkvNaH1LyR3XDXz3wjKSuP26CdaNNVYk7+xiwGPfP8sXnzzN3FLQCbkXjHlaHwm9OV4Tw0zSDMYzwC" +
            "HfxLclZsw44AaX3SCPWTbCFK+/eTRJ+GacPLfA7/2/E/zw1Dw/dcsGNo73Viw6Vui755f55svTfPPlaWYXW3jmta9Kb4NCrFOfeGp2mf/y5yc5" +
            "8u4CP3P7RiYn/AoXMSeRdiY4O7vMX752nj9+cYqZS00anmXkboNCDGbnm/yPvzjNiXOLfOL9m9i2abRneQEuzAc8deg8f/ziOd6ZWcJvWG501p" +
            "WbTL0CkrR2n2WIOTNeQrxsk59/ehxjP/CfgHvAAiQv/8pey48tF9BiJNHCUsDmCT9SRG/FTFqCqdkmFxdajPqOZStl0SHxHI+AmwtLARvGfbau" +
            "9yOXZ3XeAWJ6rsWF+SYjnoeZ8m/mcuSO8TmLzYCJUZ9rN/jOBKjOe2auyfR8E98LV4POUm4kyoNZarxKxiSiBcg8TN8H/gXioA9ckjhpcBj4AG" +
            "g0wh5YMt8ve8roRotFGx/1WFgKOLm4mIAz4cCZcBBHlrgh3DAP1vie41tQEueQ6kcQ0daOeiw1A06eW0rkLVgCXJXm3YFcNczad/aqQG5Fpddc" +
            "xO1a36MVBJyYWuwqW1Zb4asa8iVzX3BS7iQyq2xM4nQG0LzEYYOTwCU/OhZOgR0EPgZst9wbvLNn8nwwVJYWdrRzk3YnbO1CmNLD4NByzDpLFK" +
            "i2XBx8PHaeWW5hV3P64QZGlNNGkWyWCMJHyLzILvUKclTScifNbkslvloXjL8VjEma1l5Cpg0Ogk25sYDzwAvAaWB76prBAldC0BMti2DPQ6wX" +
            "OX17ubMuWzxAXZ3KykHM5WVHFMlNRbmLZVNfsllOu4VbTbw2nI50fT6+PTw+zhwGDgJ7gYku93b+mN4bN+gLVX7ceVcer3gmzUU6PhxjuDzHvD" +
            "wDPAG8o4r5HcMe1itFbaswrvGR4B3gCYW6BpDXChSfYxcknhd8H5h36yEphYPI4CK70NLPVaL10/5lzFv18Y7/nI90+zxoQcJGfcPfMTnKyalF" +
            "+Z4hOIZ4HPgQcHtOCsBwlwNW75W5gnjHxsmPgMcFx8LLuAJds2EM795b1hMEAWYy0BLou0h/ijSdrcem5EW5iUtzK9AG0UavNP1E845/mY50+l" +
            "3QUiDZaMP40K51eA/etZUdk2tYaip2LJ4GHgOeQyxXwENefQN/vN/+ZeC5SKenDawVSNdsHOHv3LMV/2/dsZk/v/0cX/nOGeSFwCfED4AvATtD" +
            "55BjJl89YV0uvIPo53XgS5FOA4AgEAfeP8mBOzbjbxz3+fyBnfzg2CyvnZijEVr/C5FFcD3wj4Dd7RtGrir/clL+UeBRxBPAQhgdhT3bxvjV+7" +
            "axadzHk+Ajt2zgV+/bxsSaRnhDaXjsOws8JvFVQrdhK+XpKIfnq3ppuaq0uttfTd4DHNcAaEmcBL4aLf1n41jYxBqPf/pz13Pv3g1RgYjo8/B9" +
            "23n12CxffeZMPAEkcQz4fYkG8HC0IgQJLGSVzlrNA2WrOAlsFSeBpS4v6XjN3wG+IvH7wDGzMIF9aVl86p5r+Lsf3c6aEY/AdZNvnxzlnz+4i/" +
            "v/xiZaQdtwDUKvEY8Aj0ZLSu+JuMMso8bq5wqsAm9XJ0cjXT0S6S4gAtXcf+smfvuBG9g4HuYDeSHcvZPrZgbfPjTD7zz6Q147PuuiUDzgRuAh" +
            "4LPAbcBI9JVcFHghPm0FtLrbX03e5YWwCttQJz2WZcHrFir/scjeDzyDpaa4c/d6/u2v7OFnP7g5jLC2QSXuGhJp9BsvnOXf/J83OXTiEiMNc3" +
            "EIW4ED0SS4F9hMSSby1c/Q6uNMAc9Gb/4T0flNBiy3xC07JvgPD+/lb9+1lZyEF2VmVyDxJwff499/7QgvH79I5CWMP2sj0/DTwAPATXSCR1cn" +
            "w/CUboTBnSPAnwF/CLwCzMcD3wzEB3et518/dDMP7r8md2WxogSOVks8/cY0v/f4UZ4+NE3LuSMv3BJsR7gK6JfAPgLsAK1zSkRYB7jsgrf7QL" +
            "DXThsW757aUPL3RK32WbBToYNHXw/ffjsFBGYh2mBhOeBjt0/yO5+6iU/euaXwbiMryw004PWTs/zHx4/y2LNnWGoKvxF3yQCtAW4A7gY7ANof" +
            "4glsEhjrFI9PwBItHRNPwxaShZDqpCWrDiWB5NXu50rJRjXZSuVWqv9xuH4BbFrodAjo4Amw54HjoEWLHmsFMLGmwQP7tvLbv7CLu27eEJ7lCp" +
            "ZkK0vhis8Ep2cW+cpTp3jkL09y+PSlEPTZ8MwMRd8fA9sG2gvsA7sL2APaHG4PNh5NFh/ML7vPvhjYUAeNAnBF9QlQXpa16m2KiT42QU1gEbgE" +
            "Nhfu8ToM9gLoReAt4AzYQlSRxVoBCiT2bh/nt35+F3/v/h1sGPepkPSqrqjc8CQZ8OKRC3zxL07w5CtTnJpeYjkI8M0MQxEg0gM2hCuAro/OCn" +
            "cadpvCvzeDrQf53cCRlqiLuxJaAi5akZZUcr80er+RsWnYRaEp4EToxrWXQK8AJyxcAS7EDjmBBYE06nts2zjKgTu28Jmf3s5Hb9vMSMPauqO0" +
            "QESXT4wYGfU9PnLLJm7ZMcFfvTrFo0+9wytvX2Rqblnzi61wBoS5GjNYMAO8F5a10iaZtoJNItZHRQSTCFcX0drGlwbJK0msg1LrlGOJ23BoFm" +
            "QRtG1aqqiPw1vmouCS7SdoFiTu5s3QCnhTwtvpv0SwHB7u7CzobUwvg/01YrYtW4jt19hIQ9duHGXfTRt46N5tfOKDW9g0MZLILOr2+f8pUwTW" +
            "z2fDxgAAAABJRU5ErkJggg==";

    private MultiprogrammingIcon() {
    }

    /** Returns the icon scaled to size x size pixels, or null if the image could not be decoded. */
    public static Icon create(int size) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(DATA)));
            if (img == null) return null;
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.drawImage(img, x, y, size, size, null);
                    g2.dispose();
                }

                @Override
                public int getIconWidth() {
                    return size;
                }

                @Override
                public int getIconHeight() {
                    return size;
                }
            };
        } catch (Exception e) {
            return null;
        }
    }
}