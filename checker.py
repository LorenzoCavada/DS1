import argparse

parser = argparse.ArgumentParser( description='Checker for the EasyCache log')
parser.add_argument('-i', dest='id', required=False, type=str, help='message id to extrapolate from the log file')
args = parser.parse_args()

def listAllIds():
  f = open("EasyCache.log", "r")
  result = []
  for x in f:
    if "MSG_ID:" in x:
      id=x.split("MSG_ID: ")[1].split(";")[0].rstrip()
      if id not in result:
        result.append(id)
  return result


def main():
  f = open("EasyCache.log", "r")
  listIds=listAllIds()
  for x in f:
    if args.id is not None:
      if args.id in x:
        print(x.rstrip())
    else:
      for id in listIds:
        if id in x:
          print(x.rstrip())

if __name__ == '__main__':
  main()