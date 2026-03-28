# 📊 Baseline Performance Analysis

## Methodology
- **Hardware/Environment**: MacBook (Architecture: aarch64).
- **Test Dataset**: `massive_input.txt` containing exactly 100,000 records.
- **Profiling Tool**: VisualVM 2.1.10.
- **Execution Strategy**: The application was run with standard Java 21 configuration. The baseline metric monitors the end-to-end data processing duration, using a custom timer that wraps `FileHandler.readBooksFromFile()` and `library.addNewBook` during the system initialization phase.

## Metrics
- **Initial Load Time (Baseline)**: **92,451 ms** (~92.45 seconds) to fully load and instantiate 100,000 book records.
- **Memory Overview**: A significant memory footprint heavily dominated by identical/duplicate `String` values. 

## Identified Hotspots
1. **CPU Bottleneck (`java.util.Scanner`)**
   - The profiling results clearly indicate that the method `Scanner.nextLine()` consumes **58.3% of the overall CPU processing time**. Relying on `Scanner` with regex-bound lookups for parsing massive continuous text data creates a huge I/O and processing delay bottleneck.
   - Significant execution time is also spent iteratively applying `addNewBook` for every record without bulk-insertion strategies.

2. **Memory Usage (Heap Overhead)**
   - The heap memory reveals **148,152 active instances of `PaperBook`**. 
   - A dramatic number of duplicate `String` instances significantly bolsters memory consumption (e.g. repeated authors, genres, boolean-equivalent strings). These redundancies point towards missing `String.intern()` usage or dictionary-based reference allocations. 

3. **Collection Inefficiencies**
   - Iterative inserts into the underlying tracking mechanisms (`ArrayList`, `HashMap`) likely trigger multiple expensive resize operations and object reallocation events since the structures are constantly adjusting to accommodate 100k records.