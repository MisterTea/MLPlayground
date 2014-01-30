import random
import math
import sys
import numpy as np

random.seed(1L)

labels = []
features = []
NUM_FEATURES = 0

#Parse libsvm
fp = open("datasets/a1a/a1a","r")
while True:
    line = fp.readline()
    if len(line)==0:
        break
    tokens = line.split(" ")
    del tokens[-1]
    labels.append(0 if int(tokens[0])==-1 else 1)
    features.append({})
    for x in xrange(1,len(tokens)):
        index,feature = tokens[x].split(":")
        index = int(index)
        NUM_FEATURES = max(NUM_FEATURES,index)
        features[-1][index-1] = float(int(feature))


def normalize(weights):
    sum = 0.0
    for x in xrange(0,len(weights)):
        sum += math.fabs(weights[x])
    if sum > 1e-6:
        for x in xrange(0,len(weights)):
            weights[x] /= sum

loss_old = 0
loss_new = 0

weights = [random.gauss(0, 1.0)]*NUM_FEATURES
eps = 0.005 # step size

NUM_INPUTS = len(features)

def logistic(x):
    if x>=100: return 0.99
    if x<=-100: return 0.01
    ret = 1 / (1 + math.exp(-x))
    return min(0.99, max(0.01, ret))


def logistic_derivative_i(x, x_i_feature):
    y = logistic(x)
    return y * (1 - y) * x_i_feature

def dot(v1,v2):
    sum = 0.0
    for x in xrange(0,len(v1)):
        sum += v1[x]*v2[x]
    return sum

def dotSparse(v1,v2):
    sum = 0.0
    for index,value in v1.iteritems():
        sum += value*v2[index]
    return sum

def printArray(v):
    print "[" + ", ".join('%+0.2f' % item for item in v) + "]"

BATCH_SIZE = NUM_INPUTS/20

count=0
gradients = np.zeros([NUM_FEATURES])
while True:
    loss_old = loss_new
    loss_new = 0
    gradients[:] = 0
    for x in xrange(NUM_INPUTS/20,NUM_INPUTS):
        #f0 = features[x][0]
        #f1 = features[x][1]
        #w0 = weights[0]
        #w1 = weights[1]

        estimate = dotSparse(features[x],weights)

        # Log loss of logistic fn
        estimate = logistic(estimate)
        #if estimate>0.5: estimate = 0.99
        #else: estimate = 0.01
        loss = -1 * ((labels[x] * math.log(estimate)) + (1-labels[x]) * math.log(1-estimate))

        #Adjust for the number of samples
        loss /= NUM_INPUTS

        loss_new += loss

        for y in xrange(0,NUM_FEATURES):
            gradient = (-1 * labels[x] * (1.0 / estimate) * features[x].get(y,0.0)) + \
                       ((labels[x] - 1) * features[x].get(y,0.0) / (estimate - 1))
            #+ (-1 * (1-labels[x]) * (1.0 / (1 - estimate)) * -1 * features[x].get(y,0.0))
            gradients[y] += gradient / BATCH_SIZE

        '''
        Better least squares gradient, takes derivative of x^2
        loss = (estimate - labels[x])**2 # Least Squared loss
        loss_new += loss

        g0 = 2 * (estimate - labels[x]) * features[x][0]
        g1 = 2 * (estimate - labels[x]) * features[x][1]
        '''

        '''
        Old least squared gradient, uses multinomial expansion
        # estimate**2 - 2 *labels[x]*estimate + labels[x]**2
        # estimate**2 = (f0 * w0)**2 + (f1 * w1)**2 + 2*f0*w0*f1*w1
        g0 = f0*w0*f0 + f0*f1*w1
        g1 = f1*w1*f1 + f0*f1*w0

        # The second part of least squares
        g0 += -1*labels[x]*f0
        g1 += -1*labels[x]*f1

        g0 *= 2;
        g1 *= 2;
        '''

        #g0 = 2*f0*w0*f0       - 2*labels[x]*f0
        #g1 = 2*f1*w1*f1       - 2*labels[x]*f1

        '''
        print 'EST',estimate,'LABEL',labels[x]
        print f0,f1
        print labels[x],estimate,w0,w1
        print g0,g1
        print '---'
        if labels[x]<0.5:
            sys.exit(0)
        '''
        #gradients[0] += g0
        #gradients[1] += g1

        if (x+1)%BATCH_SIZE == 0:
            for y in xrange(0,NUM_FEATURES):
                if abs(weights[y])<0.01 and abs(gradients[y])>0.5:
                    weights[y] -= gradients[y]
                else:
                    weights[y] -= eps * gradients[y]
            gradients[:] = 0

    if True:
        # L2 regularization
        L2_STRENGTH = 0.05
        unscaled_l2 = dot(weights,weights)
        print 'UNSCALED L2',unscaled_l2
        loss_new += L2_STRENGTH * unscaled_l2 / NUM_INPUTS

        # Partial derivative of L2 regularization
        if unscaled_l2 > 1e-6:
            for y in xrange(1,NUM_FEATURES):
                weights[y] -= eps * L2_STRENGTH * weights[y] * 2

    if True:
        # L1 regularization
        l1_strength = 0.005
        loss_new += l1_strength * math.fsum(weights) / NUM_INPUTS

        for y in xrange(1,NUM_FEATURES):
            if abs(weights[y]) < l1_strength:
                weights[y] = 0
            elif weights[y]>0:
                weights[y] -= l1_strength
            else:
                weights[y] += l1_strength
        

    print '***',count
    printArray(weights)
    print loss_new
    wins=0
    FP=0
    FN=0
    for x in xrange(0,NUM_INPUTS/20):
        estimate = dotSparse(features[x],weights)

        # Log loss of logistic fn
        estimate = logistic(estimate)

        if estimate<0.5 and labels[x]<0.5:
            wins+=1
        elif estimate>=0.5 and labels[x]>0.5:
            wins+=1
        elif labels[x]<0.5:
            FP+=1
        else:
            FN+=1
    print 'TPR',(wins*100.0)/(NUM_INPUTS/20)
    print 'FPR',(FP*100.0)/(NUM_INPUTS/20)
    print 'FNR',(FN*100.0)/(NUM_INPUTS/20)
    print '***'
    count+=1
    if abs(loss_old-loss_new) < 1e-9 and count >= 10000: break
normalize(weights)
printArray(weights)
printArray(answer_weights)


