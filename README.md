# MiniOS

A Java desktop-style learning app for operating system concepts.

## Includes

- CPU Scheduling
- Banker's Algorithm
- Paging
- Page Replacement
- Segmentation
- Fragmentation
- Disk Scheduling
- Multiprogramming

## Requirements

- Java JDK 17 or later

## Run

Open the project in VS Code, then run [Main.java](opersys/Main.java) to open the desktop.

## Simulator files

Each simulator follows the same general structure:

- `*App` launches the simulator on its own.
- `*Design` contains the Swing interface.
- `*Controller` connects the interface to the simulator logic.
- `*Model`, `*Engine`, and related classes contain validation, algorithms, and state where needed.

The desktop in `Main.java` opens simulators through their controllers. To run one separately, open its `*App` class in VS Code and select **Run Java**; for example, [FragmentationApp.java](opersys/fragmentation/FragmentationApp.java).
