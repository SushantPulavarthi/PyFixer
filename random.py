import random

def generate_random_numbers(n)
    numbers = []
    for  in range(n):
        numbers.append(random.randint(1, 100))
    return numbers

def calculate_average(numbers):
    total = sum(numbers)
    average = total / len(numbers)
    return average

num_count = input("Enter the number of random numbers to generate: ")
num_count = int(num_count)

if num_count <= 0:
    print("Error: Number of random numbers must be positive.")
    return

random_numbers = generate_random_numbers(num_count)
average = calculate_average(random_numbers)

print("Random numbers generated:", random_numbers)
print("The average of the random numbers is:", average)

