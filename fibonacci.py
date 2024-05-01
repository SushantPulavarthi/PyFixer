def fibonacci(n):
    if n <= 0:
        return 0
    elif n == 1:
        return 1
    else:
        return fibonacci(n - 1) + fibonacci(n - 2)

def generate_fibonacci_sequence(length):
    sequence = []
    for i in range(length):
        sequence.append(fibonacci(i))
    return sequence

def main():
    sequence_length = input("Enter the length of the Fibonacci sequence: ")
    sequence_length = int(sequence_length.strip())
    if sequence_length < 0:
        print("Error: Sequence length cannot be negative.")
        return
    fibonacci_sequence = generate_fibonacci_sequence(sequence_length)
    print("Fibonacci sequence up to length", sequence_length, ":", fibonacci_sequence)

if __name__ == "__main__":
    main()