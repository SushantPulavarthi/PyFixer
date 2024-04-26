# Fixed Code

def calculate_average(numbers):
    total = 0
    count = 0
    for num in numbers:
        total += num
        count += 1
    average = total / count
    return average

numbers = [10, 20, 30, 40, 50]
average = calculate_average(numbers)
print("The average is:", average)

print("\"A word that needs quotation marks\" and a single quote')")

print("Hello World!")

x = 3
try:
    y = int(input("Enter a number for division: "))
    print(x / y)
except ZeroDivisionError:
    print("Error: Division by zero is not allowed.")