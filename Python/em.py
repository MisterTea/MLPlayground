import random
import math
import sys

random.seed(1)
# From calculation, we expect that the local minimum occurs at x=9/4

def normalize(weights):
    print weights
    sum = 0.0
    for x in xrange(0,len(weights)):
        sum += math.fabs(weights[x])
    for x in xrange(0,len(weights)):
        weights[x] /= sum
    print weights


loss_old = 0
loss_new = 999
weights = [1,1]
eps = 0.001 # step size
precision = 0.00001

answer_weights = [-0.6,0.4]
normalize(answer_weights)
normalize(weights)

labels = [0]*10000
features = [(0,)]*10000

def logistic(x):
    return 1 / (1 + math.exp(-x))

def logistic_derivative_i(x, x_i_feature):
    y = logistic(x)
    return y * (1 - y) * x_i_feature

for x in xrange(0,10000):
    features[x] = (random.random(),random.random())
    labels[x] = logistic(features[x][0]*answer_weights[0] + features[x][1]*answer_weights[1])
    if labels[x]>0.5:
        labels[x] = 0.99
    else:
        labels[x] = 0.01

count=0
while True:
    loss_old = loss_new
    loss_new = 0
    gradients = [0,0]
    for x in xrange(0,10000):
        f0 = features[x][0]
        f1 = features[x][1]
        w0 = weights[0]
        w1 = weights[1]

        estimate = features[x][0]*weights[0] + features[x][1]*weights[1]

        # Log loss of logistic fn
        estimate = logistic(estimate)
        estimate = min(0.99, max(0.01, estimate))
        if estimate>0.5: estimate = 0.99
        else: estimate = 0.01
        loss = (labels[x] * math.log(estimate)) + (1-labels[x]) * math.log(1-estimate)
        loss *= -1.0 / 10000.0

        loss_new += loss

        g0 = (-1.0 / 10000.0) * labels[x] * (1.0 / estimate) * features[x][0]
        g1 = (-1.0 / 10000.0) * labels[x] * (1.0 / estimate) * features[x][1]

        g0 += (-1.0 / 10000.0) * (1-labels[x]) * (1.0 / (1 - estimate)) * -1 * features[x][0]
        g1 += (-1.0 / 10000.0) * (1-labels[x]) * (1.0 / (1 - estimate)) * -1 * features[x][1]

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
        gradients[0] += g0
        gradients[1] += g1

    print '***'
    print weights
    weights[0] -= eps * gradients[0]
    weights[1] -= eps * gradients[1]
    print gradients
    print weights
    print loss_new
    print '***'
    count+=1
    if count>100: break
normalize(weights)
print weights


