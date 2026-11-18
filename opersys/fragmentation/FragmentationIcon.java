package opersys.fragmentation;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.Icon;

/** The Fragmentation app icon (the purple tile with colored blocks), embedded so no image file is needed. */
public final class FragmentationIcon {
    private static final String DATA =
            "iVBORw0KGgoAAAANSUhEUgAAAGgAAABoCAYAAAAdHLWhAAAb2klEQVR42u19aZAc13Hml/le9TEzuO+DpEkCBAWApCgeusIaYiUR" +
            "tGFRZEgDyZK9+2MdoY1YS3ZItlbr1Xo4Dh2+fihW3rC5sevw7pqyhJFFmiJFURIkkqJELUmQEnGQAAgQAAESAHHOoKePqpfpH1XV" +
            "Xd1d3dM9FzFYVkRhCv3qVb338mW+zC/zZREmeAwOKm/YA9o6TC7+7d6bnvVG3nb1ag1knWVaAuJVAvGIoLgED1UQg32oHAtE3yDL" +
            "e+e+eODop3bc7Mf3bBtQs3s9dGiIZCLvoMkS5uu/e35RCdJvlG93Ku8xwCqBzmGynmeyTa8goEotIkAVrcsT16mNT6nf6lnTUh+A" +
            "QBG4MkQCH0SjAI4x8c8dyQ9y4Mc//X/nnZ4MobogkNJg/2Nm6PFNAQB87eNnrldrf89J8BHP5lYazoQNVQdVB1URTXAOgaBQQAGi" +
            "8LqOGEpQ0rp7SQFtbGGifuPvcW+S78I49dPuTWtrXX3UtxWqBCJmsmAysCYLJxX4Qek1w/afKQj+5x9+c+ELADDY/xM79PhtDiCd" +
            "MgINQvnPiEQV+MuPnbiabe4/k+LjGS/f67sSnPhOVUEg0nBiEojqRi1JknAA0FV57Vrr/p1sefO7JlEeUlIVqkQEw57xTA4Vv1hQ" +
            "wjclKH31899adoAA/CmUhzA+N41LoIEBNcPD5AYHlefsPfNHxN7nPZtdVPYLUIgDwBE13jqa1ihVEITAJuv1wg/Kp1X8vxxdt/Cv" +
            "h4ZI4rGdMIFCdtwU/PknT622Yv4uY3u2VIIxiAYBQCbk/y4XkcmWd7tIzFR5y75QyFRQx2RtxvagEow9HLD7D1+4b/HReIy7JtC2" +
            "ATVbh8n91dY3blE238x4PVeV/ZEAgAGYcGkqZtN4EKCiILisN9dW/LGDJO7jf7xtyTPxWHdMoLjCl7eeeG/WeP9CbBdVgqJjYvPW" +
            "QE/+EBWXsXmjEpwuO//D/2Xbsp+1IhK1Is5Xtr52i8e5R0C0KJCKI5DpeKZ0zF3d3Dvb69ffq1BnOWOgetqX0m/8ybaVqZzUQCBl" +
            "gORLA6dWWcYTlr2rKq7UBXHeOrpSIiAuY/ImEP9gIHjfF4cXH4tpkEIgpYEB8PqToMzS49/Ne/PuKPmjARHZ5pWQmo2PhBo6/eXt" +
            "2vJml3fTF0BVg5w3xxb989+vnFz+oT1LocPDkNhO4ppoAw8Pk8ssOf7ZnDf3jlJlJABgVRWdnUDn9062/ky+azrrCwDYUmUkyHlz" +
            "78gsOf7Z4WFy2wZqdKEYvrlnCPqlj528yojuIDJzVB0AcKdaMLWZY1NZf6LverPrt+srAUJkIOpGhemmL35r6cF7BkFDQyQMABv2" +
            "DBOBlAP3xYztmxc4XxVgDUkd2cbhKaqg6DdpmBmtfuumvo5Tv5t3TbZ+sq06DX2t/gawc75mbd88DtwXCaQb9gxTCIENKtMQ6Z8P" +
            "HN8oyr9QSE5VaCJA6lvH5HQGIlYCl5jkXV8YXr5LB5V4eE/IaRXnPuXZfI+qSBpxdFyNJFGLmq+Vpqi8xb0ty1Pa3x4c0Ahja312" +
            "Uj7uWKW8WlXEs/meinOfAqDDeyIM7St3HV3kM3Z6nFkh4osmlIdx4Y34jRrpHS0Eb1VvmWx5i3tblVeJpe39DfGlKkOUmtuSBpy3" +
            "XGQUTAIihcZwfKpvpf43AoTZY18qr3uC6/7kgdWnLQAE4P4MZ5f7rigg4m7ITw5QA7hehsvwRYXF2ZKAiwKSFLdF5HGLucaFyzF6" +
            "8iPoyV4IOWGCWFzZz+NCcR6cGDC5EPlOczw1/KYAi6tIxuSX+1LuB/AdGz5cNrPxSF1RKNWJ0jhHozb5gsoiD2dumoPCZVmovbig" +
            "Ly4r5rxcxPxfFWBHA8CkzT2BE4tcpohb1m/HxqueQj53AVRHUe1QJwvvDcTDgaPX4aldm3Hq3AoYE7QAbprHVAFlYxmuuBnAd+je" +
            "e9U7/sjR56zJbQxcWUCtxFv9w0gU/lyL47cvQOHK7EWLnSoB83YXsezHZ8HliCsSA65KyFgfH7hlG27Z+KNIJExSPyICyOHI0Y24" +
            "/4l/j7OjS8HkWhitzfCCNVkOXGnX8qWr32HHfn58lQIrnQRQCLVvnNZNmnMbe3HhyizYv6gxZIyuy6P3UBFzdxagXgzEK4gUgbO4" +
            "9orncNP6HwPCULWYitmmsLh89W7ceM1P8aNnPgo22iDRtOWUCmmBlWPlc6vsyDl/nQJzRH0FiJTCxb4lM4eTA0GeUFrh1Ry3F6tS" +
            "roB4wNiqLPpeGqvNMQVECYYDXLb0ANgE0MADTSy2I+W9DECweukB9GRHUKz0giGRJzYxllrj9PCaSNRXEOYUiqPrrCgtY8Oec76A" +
            "iEiBpJ6deq2AsIHY2WEqkQJiCUIAOwnVpUS/PFuanpkBguUAxvihcUq1t0rcrmQbY8KpquWMB3HLmK2sNJxp4rlWdoNWuWs2Ouzq" +
            "Wz4z/ajZRUod2pMKZfaghJVWVT2tanyKOHyjJZEi3V119hFHUYVWGszSaX5zFQXSxARH3VjXX0fqnDjPahxfpNp8U6uO1r9lVjFQ" +
            "Zwv1VL6SqsRpnA1a78BLELRqPCt33lTtGLOYHYIuSbUZEHHaKFo7O6wAEKlRrZXlkww0pJhdZxNRkoh5iuibbhmnUKi0XvXqxjpG" +
            "ugHYUHfQBiZrpSxo4i/PQs7RFNVg+leh6uhSbe1uN9YayWIBwCKdYL0J4R39EZ2NMk5TSTb93NMgSrWDsdZQtFkAEBGISKihRSHh" +
            "zfY4AA1tiNCWk1nHQSIAiSSMapoR+1pFw3ez1NDtqn8mZawVkNAdDu5soHWm5900W0ANK5BO73u1K0WktowIAAtJGFAaQ+ztJCRV" +
            "3bqz5oj6FLZZG2DFGbCENGGDaXO7kvZlTcopAAm1OI4bTwRK0c4oZRbq7GMfaB1hGtWGGVBRGhipbqyrvql6vdKiWwGnEbWdggKZ" +
            "HZELBFCggNPacproTsXPTtNLAecMfGfHXRgoRZ8UARhw3cd1kYILATKvFmdKU50U51AAeMeKoIpLiJpQhLjA4NDrV8IFGSBaxNtH" +
            "HNROtCrTeAMY4dUTV+BCsRdErut4ulBJkDqwqC58qO0pQM8LI8geLkFzodsbdHGdasMzv2sU+RdHoUwN/QCYAux/9Vo899KtIHYg" +
            "64PIdXSiVZkJwF4Frxx5G57a9V4QBPHWrlZj3TzuAMSFIk7RPli1ybkrIUHMmQrmP3QcF26ej8qVPZCsuXhEngJmLEB2XwF9z54F" +
            "FR3ABJWq9IEoQCQolzN46Mm7cPrsEly39jnM6zvfue6R8nupnMf+I9fiiV9uwulzS2BMUAtEQWeBwfG/ViDhYqUCgOu2+2n1b4q6" +
            "qgQ1gD1ZwrxHTyBY4AEex0peLaom2hMZQ0RTUt54b81JWncvVRzM2SDsnyFAJRHAUdPfiBwKxV784JnN2LH3ZvTkCuNKznbzsBJk" +
            "cOb8IvjOg2Ufok07GxLjWj/WNVhKakpCPYRDDdpaMxxeRyQbsq53qpwwvi6O7SfKCLmGQ3ZRauwX1YArcmBWnBlZiFPnFiPdVdzo" +
            "/kznCYLCmADW+JFh2uwpaD/WsVQTWHGI4requgPSo0/SIdT4v2qpw/nVWE5NNkOI83Wq86S1letQ5HqIJVlHEgp3uNPcsA9j0p7V" +
            "4vmpbQ1dDCLjQf+SPlaxvSQRWFr1V4yHx6Vdx8ZWwAkOahde3qacBTAaQkraqeLfquPd9YUIUFI4x1BldB7W31xOBDC7pHnTBT5Y" +
            "M6qpUzuo7UQOCFAGzSkDGZnUoq4lCy1kQNbNuLIhQggCi97eIrLZ8qRErx94KFzoBZFGhJpYZwSADTHSCUA3EXFoQRHeu47BXncS" +
            "1BtMikByJofgmRXwn10ZEp4xI/aVKsHLVPCOm3bj7e/YiQXzz3eaZyK1I2NjPXjpxbV4+hc3Y3S0D8a4GkjaKTQUychQxHXrtCIA" +
            "jsBzS8j9zk7YW15PbDmf+MFrBOb6k6ClYyg/fDUgXB8DNg0QnSjB2gAf3PxjbPrAE7AZP946PwkEQbH22v1Yfdlr+Pa2O1Eo9ICp" +
            "mxGuUcR2rBfVrccKdQz7nqPwbn0dWl1/JjuVLSgjyPzmAbgD8+H/cikoKwndeiJtbTeWAhd4uGbdy+jf9DNYG0AqdsrSctx463M4" +
            "dmw5Hn3k/WAv1OioSx2WBR3CDwmIRAMCzSnBrj8N5SihzlRY/6yAT6BeH7zmDEAS6guN7++mrW1OEQKzw5q1B5HJl6CBBXMYcTrZ" +
            "U5UAI1iz9hX05AtwAaVuBGsP9UjobqiJkVqKoUbNpJqVJhJlZAWU9wGhqV0oosdRjx9pdA3vr8N80/mp/l601bwIgp6eMdT2z0xR" +
            "N6JMUBmvAi9TQbGYDdukje1PGesYFnKRRzWedWiM+00N8UtiR9O9eNfidDXNTVyXDiulrW3KJxZjM/F+1LZ4JiZKXYx17ToZPxdC" +
            "PfFgUIPTpJW+HvkxdJpVYW1w5Gg7B894DqDUvsycb0uREj+kaHIgJpUEYKKhOZRAEd46ujVDuzoil3eXdpBg2kN/dSbd6tqp2je5" +
            "vminIdO13d+wDpHrtcvBmIFwvxkh0PRnutMJEwgyURFXxe6mZwAJwMwxzzSL6kk9W2Ah0r2Ii5QOmoGZNysWinEngUxcxAkE3LTh" +
            "ZHwEdzpFXLqONR3bx2lGhHVSg9O2KH/zbgdbvyBTk1upcYpRwx4bms4JqG13KiUS0bZpK9rbQTQjxEkqClT37tTdQRGniSBGEhpt" +
            "jda2gwKhM38axU81y2dLH9V4EXqdlQPa0a63KdESFWgMjGvZyjhwBBIqCdrCrtaGWdB4Pa09o9Zir5u2jls+3WupapPJ2K59jddR" +
            "0IjUJfdutUUi2TlxgPqMtjlTJkEcrcQRONrUpurbxmsrNRvqafUrvpkWugBh4GLgOMpuhQS22ND+urbW1GybZKk0N4imSQdWyIiH" +
            "4EgfsjedCl1/PEUCmwGtGPivzIH4AGU1dZDTGpfW1nYzl6AIAovDh5dXfU86ZfhVmE386NFlGB3NR067NtpjHQQ6WagHgDqgsH0F" +
            "/H3zQD1BiDxP8iRPQXmH0rOLUN65AGRlWuWoarglZOcLa7Hj6fWgjA+yDmQmf3K+jNcPrcCTP70+ZT3tBuqJPardPsAq/EO9OHfv" +
            "Nej7yCFk1o6GA92WPdrPTi0alJ5fhMIDl8OdtyCjgMOUugEaD2aHkZEc/ukbt2N0NI8b3r4fuVxlUs8MAoODB1fhew+/B0eOLE9w" +
            "j3Y8c0JNWaKYhCqsQt2JI8+hsn8uzn5tPeyKIrjPn7i9ogR3NgN3Ih96aJOpU7QTAk+sXJVgreDs2T780zdux49+eCsWLBidOOcS" +
            "UCjkceLEQpSKGTC77hXemOMEjf4gdJ0CjFggJYPKy301z+pE7UkK1RZiqYbotlUZp6hcFWAWOMc4dmwxjh5djMlm/TUmiuiJPwHT" +
            "1VjEe1QlGfqLzgI0UhY2Yg3d1TTJGa5UpwF1j8l0Xt4ocUJ3mMBYl9wh2TCBWgRnqjYNtirVb83uItSwqvFhsnFxDaJCpQvDppPy" +
            "qaNH06u41RglP3oU0aFqONelK4gwCiIQUcIbGt7DHGUlo4nZ9PEmhQaop/skgwLAOYJlRcbMDg+eKFAKGATAcOs2EwESAIEvsB4j" +
            "m2cYy3XllYoiKEuVEMYjeBmD0piDXxawIRhLqf7N9hJOIrBUUE0d3O2kDBTozQhuvaKADcuLyFqZsnzunUG2EysXACdHPfy/w304" +
            "cCqbyttEQFBRzF+SwXu3LEXPXItKyWHd2+eCOMLJPMKTD72Bx79zHNkeg8qYw3XvXoTbP7ESpbEAr79SxFPfP4VjBwtgpo4lXE3+" +
            "Sk3EdbU/KGLlnBV89IYzuO2aEbAVzJ5MzuF6d/MVF3Df04vxzJHearbMeIdnECiWrMrhdz9/JW749YU4c7KMYwfHsOFd8+uedGDn" +
            "Bbhoa6VzioXLMtjwznlh4SbghvctwN//2cs4sHMUNsNo1MVa7g+qBs+L1KVIaWV51+0nIKDiCJvWXsC/uXYEKoD4synzSDgUC/oC" +
            "fPj6szh4OoM3Ch5sBCuJANk8Y+tnrsANv74QADBnvodc3sAFCmMpDG4ngnOhMR07MJ2rjaBzisvW9OKuT12Gv/lPe1EpCZjRtOki" +
            "RUeo+YPAABzqNxC1hUcAJ8CcrMN1q8aq8AjPwjToGhBWzqvgqsVlnBj1ABtOvnLJ4dYPLsbb37cQ4sKRObKvgGd/fBov7jgPawki" +
            "gJch7H1+BGwJKgrjMY7sL+AH33wN62+eh9VregEAS1fnsWRlFq/uHwMzdQCSJnd5S3cBGvHWwYwV9GXc9KPB08xHTMDcnKv6uQCC" +
            "OMWvXduLTDaUCqePl3Hvf92HI/sLMFwP0BpLsB7BuZCzDu4axZ6nz2LjuxfgA1tX4tX9F7Dv+RGcer0MNujMq5rcfiIitf36HRBJ" +
            "pt8d9CawUv23FazH2Pv8eXgZhqji+KEijh8ZQzbHTaZe6FiLI0IVxIT8HIsDO0dx+KV9KBYcRBRehkGMcUyRpH0ZQT0TxGlnZTqY" +
            "tpppwo5hA+x47DSe2X46QksAY6ij2Z8MQvErikwuVOdFukw5U9thh3qwtANDaNZmXGylLsQzVmrTjg3BejUoKOaSjmErhJqglMIf" +
            "jSUYQx3Xj77wmYB62kESDZQV4BLjoFqCjnie+iWHINAqseL1qDNDJiTI+7Ysw5IVOagq9v1qBDufOgsvwzVm0HYQ2mS3QOqlI94a" +
            "uyKiuPyaPiy7PA8VxcjZAAd3j3TMkiohgT74sZVYs3EuAODh//MqnnviNDJZ7krUWZFEMqXO19RaZgy6ZHioOgbOF7x/YAU2//Zq" +
            "AMCLO87hS7/3y45FZojbKTRIaHyKuv1BnbkbAlgiZUizLGyTJQCdSsPZxEYau+0j3DPwFSKhmAn8muijlDWkaawSGQCqEFr123Xj" +
            "j7VWiWTYSiCngKCeqtpakmki3c0lpyRoYgMVEvkYky5rarF2UAO3IMTqKLLgjanlG08d6zrUm+A0gJCcss7JSaKKapyieZyBp2qu" +
            "H8Glc2i901IFbAnxl5SMV5vq46Xnjx3TfiB46tETOPTSKFQVL+04B2NjlHo8JUFYRDXLdNLmer2DpaJfgJq+KKEadSxoL9GDGDj8" +
            "0ih++eRpiChe2TNaS+fasBY3IuA1RUPw3X84XDVMiQg2qcG1W+HJkEIuePncQbvsst4Dh/aePcfgPtHO8hzEiUD0kuCdxgU8VKuf" +
            "fPg4fvHoiSj5rsIFWm8LEZrsGhdoIh5OYS3BeFz37E5axMQQDc4tWJQ5YP/i2zef37r+B7vI8GpRjaMKxod8ROHk0iCSL0hAPeFv" +
            "lbJDpRStDU6xcFkOC5ZmEQQCYkJQEbxxrIhK5KwDCMsv70Gu18B6jMvX9mLv8+dxaO8osrnOVWuFqiEmFd31F9+++byN5Op2grmD" +
            "FG2+wFWbcUzASIlx9KzF+tVlOA1/m00at2goyio+4dh50+ztJFTXIFFgy7+9HP0fXoHAFxjLOHl0DF//wm4ce6WATI4hovjEZ6/G" +
            "tTcugKpi7vwMDuw5j7//yj4c3D3S8UYxUgjBsDK2o/q1R6ZHK+5CSQGj4THO/n1FOQAe25/D2fMGJqOzzhxiBsgonnw5hwMnLSxp" +
            "hJfV51solx3WXD8Xmz6yEvMWZbBoeQ7zF2ewYGm2Hp0WxbyFmbBsSRbGI1xzw3x8/NNXo2+erfMbtT5FFWQq7kIJSo8C4deGaf3O" +
            "J3cr5GlDWZIoqXy7UxTIGMW+kx7+x8/n4qWjWZR8QqfZNN/s0ylw5gLjkV/14FvP9aIcUJh8orGfAjATVq/pgzhFpeRQKQtcoCgX" +
            "BZJIgqIAKqVQIygWApx4tYhf/PAE7v9fr2CsEICYkPwASuq4gsRQhhTy9PqdT+5WVaKBATXDw+QG1n//E8z5+3wpOSIy6cKN6tRt" +
            "AKgIsDAvuGaZj96szBrxdvy8xctveBANA0e0yVSsRer0zLG47OrekGuYwAyMXXDY8+xZFAtB6IRTxcZbF2LhsizKYw6vHR7D0YMX" +
            "UCkprEfRPWj4yGG9YiyqzuOc8aX4ye/sueMbAwNqCAANDg7Sgfvfkx/z5SnLuY2BVCRMeDm+uk0AnBJ8N7s0b8OAZ3R8iyFygTtf" +
            "4FzNYCEKAVTimoVZKQvEhT4hYwnWUpVzxhscBTnLGQ6ktKvH43dffffPi0NDQ2FCyz17NvDwC5sLH7n2ka8q8I14p8O40TIab2xQ" +
            "5O30R+NM5QZIQYstLWj6qDCYAJPj6qynqqivxXIQgEyeQRH+EH9eBqId9SUyk4kIX/3HFzYXBtZtM4gS7QAABqE8BJK71j38w4zp" +
            "/UBFxhwTmWQUSgz1UJuRmPbyVm15s8sn0VeBuozpMZWg8KMH9m75YEwL1Lsb7gFA8Az9QSClnzGZeaquuvOnLqiuncthustbteXN" +
            "Lp94X4XJcOBK5zxDfxBS8p56rDA+BrDNDGOru3vddz9pOP+PTitBmCE7qcVP98e6p/Bj3jNaPpG+qALkDGWsk+Lv3L/3Q/fFNKiu" +
            "lcnb92BY+/t/Yr+/Y/Ov1i7cmvW4t99JxaFpo9cEE9BOWTku0vIu+6JwHvda34199YF9H/paf/9P7PcO/5arU2YaH3f48D/owMAG" +
            "888/vWv7fQt2L/VM7ztFKkH0bRR6K3/S5N0bClVSOGt6re8Kf/vA/i2f2zOwwXzve1sEGGp2h6Q/J9TT7lzz8H+zNvfpQEoKVSUi" +
            "1nZcy0h8Xnccrk/eOxEJl1a/Gwk1lfU76Gv0cUYBEVnOUSClrz+4b8tnahurmt/G7fh2YGCbefDlLZ8JpPA5KPtMlkUlCHevaOTb" +
            "aDglcZ1Wri3uTStPq49x6uNNqt+mr4BCRVREAibLUPYDv/C5B/dt+czAwDbTTpaOu2lnEEpDIPmta+5/P8T+d8/0rAukCIW4cJsN" +
            "0VuCq71/RwEhsLGcRyBje5WC//jQvru3R+p0W8/NeBHvOgSS/v5B+9C+u7fnJffOQMe+DKXzlnoMwKQqgapKJyDr/0enhmMiAcBk" +
            "qcdA6XygY1/Ozc2986F9d2/v7x+0ka2j461ZHR1J9e/OX3tgnVj7+6T4mOHsEoGDig8JuSqO9SFKfNWDEklU4xw6tdSqaeXpV43X" +
            "mGR5Z20Zv63hlx9EQ3cbGWIPDAsnxTeU6FscBH/z4KG79jaOZSdKRTfsSgMD4OFhcgCwZe23r2L17nSKu1jpRubsXCYPqn4iNbde" +
            "lFDPVLYFYBAxCAaCAOLKI0r6PBMeEPIffHj/Rw+Ga7qa4WFIN3kFJrR+DGKQ9wzcQzGhAODOdQ+sg5gbncjboHibQJYxYRmUbJ3x" +
            "zQqKESbWJNoUlcffaU3XsuqgluSzUssTz5pQ/fblIQNpIMAJgE8w4UXD+iIYzz+4N+SWmDDrh+/RIQx1DfdPcoEf5P7+2/i22x6T" +
            "oaHmlw/+u5/kkB3lS1oHKM+Rof+9qdTU98FBfuyx2/jxxx8TTIAw8fGvIo4Vod2BIAgAAAAASUVORK5CYII=";

    private FragmentationIcon() {
    }

    /**
     * Returns the icon scaled to size x size pixels, or null if the image could not be decoded.
     * The picture is re-cut with a smooth rounded-corner mask (and trimmed a few percent at the edges), so no
     * white corner or border can show around the tile, whatever the source pixels are.
     */
    public static Icon create(int size) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(DATA)));
            if (src == null) return null;

            BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(0.5f, 0.5f, size - 1f, size - 1f, size * 0.47f, size * 0.47f));
            g.setComposite(AlphaComposite.SrcIn);   // keep the picture only inside the rounded shape
            int d = Math.max(1, Math.round(size * 0.03f));
            g.drawImage(src, -d, -d, size + 2 * d, size + 2 * d, null);
            g.dispose();

            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g2, int x, int y) {
                    g2.drawImage(out, x, y, null);
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