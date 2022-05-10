# Louvain Algorithm in Kotlin

This repository contains an implementation of the Louvain algorithm in Kotlin.

Louvain algorithm is a greedy optimization method for community detection introduced by Blondel _et al._ in 
[this paper](https://arxiv.org/abs/0803.0476).

## Algorithm

The algorithm optimizes modularity, which is computed as follows:  
![equation](https://latex.codecogs.com/gif.latex?Q&space;=&space;\frac{1}{2m}\sum\limits_{ij}\bigg[A_{ij}&space;-&space;\frac{k_i&space;k_j}{2m}\bigg]\delta&space;(c_i,c_j))  
Where  
![Aij](https://latex.codecogs.com/gif.latex?\large&space;A_{ij}) represents the edge weight between nodes i and j;  
![k](https://latex.codecogs.com/gif.latex?\large&space;k_i,&space;k_j) are the sum of the weights of the edges attached 
to nodes i and j;  
![m](https://latex.codecogs.com/gif.latex?\large&space;m) is the sum of all the edge weights in the graph;  
![c](https://latex.codecogs.com/gif.latex?\large&space;c_i,&space;c_j) are the communities of the nodes i and j;  
![delta](https://latex.codecogs.com/gif.latex?\large&space;\delta&space;(x,y)) is 1 if arguments are equal, 0 otherwise.

In the first step, each node gets assigned to its own community. 
At each step the algorithm tries to move each node to a community which fits the node best, based on modularity changes caused by this move. 
This process is repeated for all nodes until no further modularity increase can occur.
In the second step, the algorithm builds a new graph with nodes being communities consisting of nodes of the previous graph, to which the first step can be applied again.

## Usage
This repository provides two functions in the [API](src/main/kotlin/org/jetbrains/research/ictl/louvain/API.kt) file.
Both expect a list of objects implementing [Link](src/main/kotlin/org/jetbrains/research/ictl/louvain/Link.kt) interface
as their first argument.

`getPartition` takes the desired depth of partition as an argument. 
Depth is the number of iterations of the algorithm on the resulting communities after getting the first partition. 
A map `node id` : `community id` is returned.

`computeModularity` expects a community split as its second argument. 
Computes and returns modularity of the given split.

## Issues

If you encounter any issues, please report them using the [issue tracker](https://github.com/JetBrains-Research/louvain/issues).