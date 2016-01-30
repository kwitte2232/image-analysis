
def three_n_plus_one(i, j):

    max_cycle = 0
    for digit in range(i, j+1):
        cycle = get_to_one(digit)
        if cycle >= max_cycle:
            max_cycle = cycle

    string = str(i) + " " + str(j) + " " + str(max_cycle)
    return string


def get_to_one(digit):

    if digit == 1:
        count = 1
        return count

    elif digit % 2 == 1:
        digit = 3 * digit + 1
        count = get_to_one(digit)
        count = count + 1

    elif digit % 2 == 0:
        digit = digit/2
        count = get_to_one(digit)
        count = count + 1

    return count