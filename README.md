## Screenshot link

- [LINK](https://docs.google.com/document/d/1nMui8qIu-kQMKUfR59ld5_vSFDCTAJJnKafUPUqoU38/edit?tab=t.0)

## What is included

- CPU Scheduling
- Banker's Algorithm
- Paging
- Page Replacement
- Segmentation
- Memory Fragmentation
- Disk Scheduling
- Multiprogramming

## Requirements

- Java JDK 17 or newer
- Visual Studio Code with the Java extensions

## How to ruN

Open the repository folder in VS Code, then run the `opersys.Main` 

You can also run the project from a terminal using

bash
javac -d out_run opersys/*.java opersys/*/*.java



## Modules

### CPU Scheduling

Supported algorithms:

- FCFS - First Come, First Served
- SJF - Shortest Job First
- Priority (non-preemptive)
- SRTF (preemptive SJF)
- Preemptive Priority
- Round Robin

Formulas:

```text
Turnaround time = finish time - arrival time
Waiting time = turnaround time - burst time
```

### Banker's Algorithm

You enter:

- Available resources
- Maximum resource need for each process
- Allocated resources for each process

The app calculates:

```text
Need = Maximum - Allocation
```

The safety check repeatedly searches for a process whose Need is less than or equal to the currently available resources.

The module also supports resource requests. A request is granted only when:

1. It does not exceed the process's Need.
2. It does not exceed the currently available resources.
3. The resulting state is still safe.

If the request would make the system unsafe, it is denied and the attempted allocation is rolled back.

### Paging

Address translation uses:

```text
Page number = logical address / page size
Offset = logical address % page size
Physical address = (frame number * page size) + offset
```

The module reports an address as invalid if it is outside the process size.

### Page Replacement

Page Replacement is used when the memory frames are full and a page fault occurs. The app models each frame as an open tab and records hits, faults, and victim pages.

Supported algorithms:

- FIFO - First In, First Out
- LRU - Least Recently Used
- LFU - Least Frequently Used
- OPT - Optimal:

The module tracks both hits and page faults.

### Segmentation

- A name
- A base address
- A limit, or maximum segment size

Address translation uses:

```text
Physical address = segment base + offset
```

The offset must be non-negative and smaller than the segment limit. Otherwise, the translation creates a segmentation fault:

- Offset outside the limit: segmentation fault
- Segment number does not exist: no segment / invalid segment
- Offset is valid: physical address is returned

### Memory Fragmentation


Supported algorithms:

- First Fit
- Next Fit
- Best Fit
- Worst Fit

The module also supports delete and reset actions. Deleting a process adds itsmemory back to the free space, while reset clears the full memory.

External fragmentation: happens when there is enough free memory overall but it is split into holes that are too small individually. Compact moves all allocated pieces together and turns the free space into one large hole.

The app reports:

- Free memory
- Biggest free hole
- Number of holes
- Fragmentation percentage

### Disk Scheduling

Supported algorithms:

- FCFS
- SSTF
- SCAN
- C-SCAN
- LOOK
- C-LOOK

The app shows each stop and calculates total head movement

```text
Total movement = sum of absolute distances between consecutive head stops
```

### Multiprogramming

Multiprogramming shows how jobs share limited memory and a CPU over time.

It has:

- A name
- A memory requirement in KB
- A burst/CPU time

```text
Execution slice = minimum(quantum, remaining burst time)
```

The module displays:

- Running, ready, waiting, and finished states
- Current memory usage
- Ready queue
- Step by step events
- Maximum degree of multiprogramming
- Time at which every job finishes

## App structure

The project is organized by package:

```text
opersys/
  Main.java                 This start the virtual OS 
  VirtualDesktop.java       The desktop window and app launcher
  core/                     The ui and layout of the OS
  cpuscheduling/            CPU scheduling app
  bankers/                  Banker's Algorithm app
  paging/                   Paging Algorithm app
  pagereplacement/          Page replacement algorithm app
  segmentation/             Segmentation app
  fragmentation/            Fragmentation app
  diskscheduling/           Disk Scheduling app
  multiprogramming/         Multi programming app 
```

Most modules follow this pattern:

- `*App` opens a standalone window
- `*Design` builds the Swing interface
- `*Controller` connects the interface to the logic
- `*Model` or `*Engine` validates input and performs calculations
- `*History` or `*Step` stores and explains previous actions
- `*Icon` draws the desktop icon


