import numpy as np
import matplotlib.pyplot as plt


N = 50
C = 10
pointCategories = np.zeros([N], dtype=np.int64)
pointCategories[25:] = 1

pointColors = []
for x in xrange(0,N):
    if pointCategories[x]==0:
        pointColors.append('r')
    else:
        pointColors.append('b')

SCALE = 10
points = np.random.rand(N,2)*SCALE

clusters = np.random.rand(C,2)*SCALE

clusterCategories = np.zeros([C], dtype=np.int64)
clusterCategories[5:] = 1

clusterColors = []
for x in xrange(0,C):
    if clusterCategories[x]==0:
        clusterColors.append('r')
    else:
        clusterColors.append('b')

pointClusterMap = np.zeros([N], dtype=np.int64)

def distanceSq(p1,p2):
    print 'DISTANCE',p1,p2
    print (p1[0]-p2[0])*(p1[0]-p2[0]) + (p1[1]-p2[1])*(p1[1]-p2[1])
    return (p1[0]-p2[0])*(p1[0]-p2[0]) + (p1[1]-p2[1])*(p1[1]-p2[1])

for iteration in xrange(0,10000):
    # Move unused clusters
    for c in xrange(0,C):
        if clusters[c,0] == 0 and clusters[c,1] == 0:
            clusters[c,0] = np.random.random()*SCALE
            clusters[c,1] = np.random.random()*SCALE
    print clusters

    # Assignment step
    for p in xrange(0,N):
        point = points[p];
        bestDistance = None
        pointClusterMap[p] = -1
        for c in xrange(0,C):
            d = distanceSq(point, clusters[c])
            if bestDistance == None or d < bestDistance:
                bestDistance = d;
                pointClusterMap[p] = c
        print bestDistance
        print pointClusterMap[p]
    
    # Update step
    clusters[:] = 0
    clusterMapCounts = np.zeros([C], dtype=np.int64)

    for p in xrange(0,N):
        c = pointClusterMap[p]
        if pointCategories[p] != clusterCategories[c]:
            continue
        clusters[c] += points[p]
        clusterMapCounts[c] += 1

    for c in xrange(0,C):
        if clusterMapCounts[c]>0:
            clusters[c] /= clusterMapCounts[c]

    print clusters

    scatterPlot = plt.scatter(x=points[:,0], y=points[:,1], c=pointColors, s=np.pi * 5 * 5, alpha=0.5)
    scatterPlot = plt.scatter(x=clusters[:,0], y=clusters[:,1], c=clusterColors, s=np.pi * 10 * 10, alpha=1.0)
    plt.show()
