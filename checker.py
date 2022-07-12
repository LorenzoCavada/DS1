import argparse

parser = argparse.ArgumentParser( description='Checker for the EasyCache log')
parser.add_argument('-i', dest='id', required=True, type=str, help='message id to extrapolate from the log file')
args = parser.parse_args()

def main():
  f = open("EasyCache.log", "r")
  for x in f:
    if args.id in x:
      print(x.rstrip())

if __name__ == '__main__':
  main()