import { createContext } from 'react';
import { RouteObject } from 'react-router-dom';

export const RoutesContext = createContext([] as RouteObject[]);
