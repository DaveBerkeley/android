#!/usr/bin/python

import json

class Control:
    def __init__(self, klass, *args, **kwargs):
        self.d = [ klass, kwargs, ]
    def json(self):
        return self.d

#
#

def GridView(elements, *args, **kwargs):
    cols = 0
    for col in elements:
        cols = max(cols, len(col))
    rows = len(elements)
    # re-order into a linear sequence
    seq = []
    for row in elements:
        for i in range(cols):
            if i < len(row):
                seq.append(row[i] or TextLabel(""))
            else:
                seq.append(TextLabel(""))

    c = Control("GridView", elements=seq, rows=rows, cols=cols,  **kwargs)
    return c.json()

def Page(name, elements, *args, **kwargs):
    c = Control("Page", title=name, elements=elements, **kwargs)
    return c.json()

def Button(text, topic, send, *args, **kwargs):
    c = Control("Button", text=text, topic=topic, send=send, **kwargs)
    return c.json()

def TextLabel(text, *args, **kwargs):
    c = Control("TextLabel", text=text, **kwargs)
    return c.json()

def TextView(topic, field=None, *args, **kwargs):
    c = Control("TextView", topic=topic, field=field, **kwargs)
    return c.json()

def CheckBox(topic, field=None):
    c = Control("CheckBox", topic=topic, field=field)
    return c.json()

def ProgressBar(minf, maxf, topic, field=None):
    c = Control("ProgressBar", min=minf, max=maxf, topic=topic, field=field)
    return c.json()

def SeekBar(minf, maxf, topic):
    c = Control("SeekBar", min=minf, max=maxf, topic=topic)
    return c.json()

def Bell(topic, field=None):
    c = Control("Bell", topic=topic, field=field)
    return c.json()

def GPS(topic):
    c = Control("GPS", topic=topic)
    return c.json()

def Url(topic, text="text", url="url", *args, **kwargs):
    c = Control("Url", topic=topic, text=text, url=url, **kwargs)
    return c.json()

def EditText(topic, field=None, *args, **kwargs):
    c = Control("EditText", topic=topic, field=field, **kwargs)
    return c.json()

def WebView(topic, field=None, *args, **kwargs):
    c = Control("WebView", topic=topic, field=field, **kwargs)
    return c.json()

#
#

water = [
    [
        TextLabel("x"),
        TextView("home/jeenet/magnetometerdev_12", "x", post=" mGs"),
    ],
    [
        TextLabel("y"),
        TextView("home/jeenet/magnetometerdev_12", "y", post=" mGs"),
    ],
    [
        TextLabel("z"),
        TextView("home/jeenet/magnetometerdev_12", "z", post=" mGs"),
    ],
]

watermeter = [
    TextLabel("Water Meter"),
    GridView(water),
    TextView("home/jeenet/magnetometerdev_12", "time"),
    TextView("home/jeenet/magnetometerdev_12", "vcc", post=" V"),
    TextView("home/jeenet/magnetometerdev_12", "temp", post=" C"),
]

#
#

temp = [
    [
        TextLabel("Office"),
        ProgressBar(0.0, 30.0, "home/node/105", "temp"),
        TextView("home/node/105", "temp", post=" C"),
    ],
    [   
        TextLabel("Office - 9V"),
        ProgressBar(0.0, 30.0, "home/jeenet/voltagedev_10", "temp"),
        TextView("home/jeenet/voltagedev_10", "temp", post=" C"),
    ],
    [
        TextLabel("Front Room"),
        ProgressBar(0.0, 30.0, "home/node/104", "temp"),
        TextView("home/node/104", "temp", post=" C"),
    ],
    [   
        TextLabel("Front Room (test)"),
        ProgressBar(0.0, 30.0, "home/jeenet/testdev_1", "temp"),
        TextView("home/jeenet/testdev_1", "temp", post=" C"),
    ],
    [
        TextLabel("Front Bedroom"),
        ProgressBar(0.0, 30.0, "home/node/109", "temp"),
        TextView("home/node/109", "temp", post=" C"),
    ],
    [
        TextLabel("Back Room (esp)"),
        ProgressBar(0.0, 30.0, "home/node/108", "temp"),
        TextView("home/node/108", "temp", post=" C"),
    ],
    [   
        TextLabel("Back Room (humidity)"),
        ProgressBar(0.0, 30.0, "home/jeenet/humiditydev_2", "temp"),
        TextView("home/jeenet/humiditydev_2", "temp", post=" C"),
    ],
    [   
        TextLabel("Porch"),
        ProgressBar(0.0, 30.0, "home/jeenet/voltagedev_9", "temp"),
        TextView("home/jeenet/voltagedev_9", "temp", post=" C"),
    ],
    [   
        TextLabel("Car"),
        ProgressBar(0.0, 30.0, "home/jeenet/voltagedev_11", "temp"),
        TextView("home/jeenet/voltagedev_11", "temp", post=" C"),
    ],
    [   
        TextLabel("Water Meter"),
        ProgressBar(0.0, 30.0, "home/jeenet/magnetometerdev_12", "temp"),
        TextView("home/jeenet/magnetometerdev_12", "temp", post=" C"),
    ],
    [
        TextLabel("Servers:"),
    ],
    [   
        TextLabel("Gateway"),
        ProgressBar(0.0, 50.0, "home/jeenet/gateway", "temp"),
        TextView("home/jeenet/gateway", "temp", post=" C"),
    ],
    [   
        TextLabel("klatu 0"),
        ProgressBar(0.0, 50.0, "home/net/klatu", "temp_0"),
        TextView("home/net/klatu", "temp_0", post=" C"),
    ],
    [   
        TextLabel("klatu 1"),
        ProgressBar(0.0, 50.0, "home/net/klatu", "temp_1"),
        TextView("home/net/klatu", "temp_1", post=" C"),
    ],
]

temperature = [
    TextLabel("Temperature"),
    GridView(temp),
]

#
#

keys = [
    [
        Button("1", "uif/kb", "1"),
        Button("2", "uif/kb", "2"),
        Button("3", "uif/kb", "3"),
    ],
    [
        Button("4", "uif/kb", "4"),
        Button("5", "uif/kb", "5"),
        Button("6", "uif/kb", "6"),
    ],
    [
        Button("7", "uif/kb", "7"),
        Button("8", "uif/kb", "8"),
        Button("9", "uif/kb", "9"),
    ],
    [
        Button("*", "uif/kb", '"*"'),
        Button("0", "uif/kb", "0"),
        Button("#", "uif/kb", '"#"'),
    ],
    [
        None,
        Button("CLR", "uif/clr", "1"),
    ],
]

keyboard = [
    TextLabel("Keyboard Example"),
    TextView("uif/result"),
    GridView(keys),
]

#
#

controls = [
    ProgressBar(0.0, 180.0, "uif/seek/1"),
    GridView([
        [   
            TextLabel("Car Battery"),
            ProgressBar(11.0, 13.0, "home/jeenet/voltagedev_11", "voltage"),
            TextView("home/jeenet/voltagedev_11", "voltage", post=" V"),
        ],
        [   
            TextLabel("Car Temp."),
            ProgressBar(0.0, 35.0, "home/jeenet/voltagedev_11", "temp"),
            TextView("home/jeenet/voltagedev_11", "temp", post=" C"),
        ],
        [
            TextLabel("River Levels"),
        ],
        [   
            TextLabel("Newlyn"),
            ProgressBar(-2.5, 2.8, "rivers/level/3156", "level"),
            TextView("rivers/level/3156", "level", post=" m"),
        ],
        [   
            TextLabel("Devonport"),
            ProgressBar(-2.5, 2.8, "rivers/level/3344", "level"),
            TextView("rivers/level/3344", "level", post=" m"),
        ],
        [   
            TextLabel("Richmond"),
            ProgressBar(-1.0, 4.8, "rivers/level/7393", "level"),
            TextView("rivers/level/7393", "level", post=" m"),
        ],
    ]),

    TextLabel("Gas Meter (sector)"),
    ProgressBar(63.0, 0.0, "home/gas", "sector"),
    
    GridView([
        [
            TextLabel("Relay"),
            Button("On", "rpc/jeenet", '{"device": "relaydev_7", "fn": "set_relay", "args":[1]}'),
            Button("Off", "rpc/jeenet", '{"device": "relaydev_7", "fn": "set_relay", "args":[0]}'),
            CheckBox("home/jeenet/relaydev_7", "relay"),
            TextView("home/jeenet/relaydev_7", "temp", post=" C"),
        ]
    ]),

    SeekBar(0.0, 180.0, "uif/seek/1"),

    TextLabel("Export"),
    ProgressBar(0.0, -3000.0, "home/power", "power"),
    TextLabel("Import"),
    ProgressBar(0.0, 3000.0, "home/power", "power"),
    TextView("home/power", "power", post=" W"),

    GPS("uif/gps/%I"),

    Url("url/alert/dave", fontsize=20, textcolor="blue"),
    TextLabel(""),
    GridView([
        [
            Button("Power Delta", "uif/button/power_delta", "1"),
            CheckBox("uif/power_delta"),
            TextView("uif/power_max", post=" W"),
            Bell("bell/1"),
        ]
    ], fontsize=18, textcolor="red"),
    TextLabel("first"),
    TextLabel("another"),
    TextLabel("FIN"),
]

#
#

def make_chat(title, ident, other):
    chat = [
        TextLabel(title),
        TextView("uif/text/" + other, fontsize=25),
        EditText("uif/text/" + ident),
    ]
    return chat

chat1 = make_chat("Chat 1", "maggie", "dave")
chat2 = make_chat("Chat 2", "dave", "maggie")

#
#

web = [
    WebView("url/alert/dave", "url"),
]

#
#

pages = [
    Page("Main Page", controls),
    Page("Web View", web),
    Page("Chat 1", chat1),
    Page("Chat 2", chat2),
    Page("Temperature", temperature),
    #Page("Water Meter", watermeter),
    Page("Keyboard", keyboard),
]

#
#

print "Content-type: application/json"
print
print json.dumps(pages)

# FIN
