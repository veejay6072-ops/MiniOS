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

Open the repository root in VS Code, then select **Run MiniOS** in Run and Debug.
The entry point is `opersys.Main` because `Main.java` declares the `opersys` package.

## Simulator files


- `*App` launches the simulator on its own.
- `*Design` contains the Swing interface.
- `*Controller` connects the interface to the simulator logic.
- `*Model`, `*Engine`, and related classes contain validation, algorithms, and state where needed.

