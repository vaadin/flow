import router from 'Frontend/generated/flow/routes.js';
import { RouterProvider } from 'react-router-dom';

export default function App() {
    return <RouterProvider router={router} />;
}
